package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.INSTALL_SOURCE_INFO_CLASS;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER;
import static icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstOrNullWithType;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.lastWithType;

import android.content.pm.PackageInstaller;
import android.os.Binder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class InstallerHookTarget33 extends InstallerHookTargetBase {

    public InstallerHookTarget33() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super("InstallerHookTarget33");
    }

    InstallerHookTarget33(String tag) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super(tag);
    }

    private final List<String> androidPkgClazzNames = List.of("AndroidPackage", "PackageImpl");

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                COMPUTER_ENGINE_CLASS,
                "isCallerInstallerOfRecord",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);
                    final var callingUid = lastWithType(args, int.class);

                    final var result = applyInstallerHiding(methodName, callingUid, () -> {
                        for (int i = args.length - 1; i >= 0; i--) {
                            final var item = args[i];

                            final var simpleName = item.getClass().getSimpleName();
                            if (androidPkgClazzNames.contains(simpleName)) {
                                try {
                                    return (String) callMethod(item,
                                            "PackageImpl".equals(simpleName)
                                                    ? "getManifestPackageName"
                                                    : "getPackageName"
                                    );
                                } catch (Throwable ignore) {
                                    return null;
                                }
                            }
                        }

                        return null;
                    });

                    switch (result) {
                        case FAKE_INSTALLATION_SOURCE_USER -> {
                            final var uid = psPackageInfo != null && psPackageInfo.applicationInfo != null
                                    ? psPackageInfo.applicationInfo.uid
                                    : -1;
                            returnValue.setResult(callingUid == uid);
                        }
                        case FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.setResult(false);
                    }
                }
        );

        service.hooker.hookBefore(
                COMPUTER_ENGINE_CLASS,
                "getInstallSourceInfo",
                (methodName, frame, returnValue) -> {
                    final var result = applyInstallerHiding(methodName,
                            Binder.getCallingUid(), () -> (String) getArgument(frame, 1));

                    switch (result) {
                        case FAKE_INSTALLATION_SOURCE_USER -> returnValue.setResult(fakeUserPackageInstallSourceInfo);
                        case FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.setResult(fakeSystemPackageInstallSourceInfo);
                    }
                }
        );

        service.hooker.hookBefore(
                COMPUTER_ENGINE_CLASS,
                "getInstallerPackageName",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);
                    final var result = applyInstallerHiding(methodName,
                            Binder.getCallingUid(), () -> firstOrNullWithType(args, String.class));

                    switch (result) {
                        case FAKE_INSTALLATION_SOURCE_USER -> returnValue.setResult(VENDING_PACKAGE_NAME);
                        case FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.setResult(null);
                    }
                }
        );
    }

    @Override
    protected Object getFakeSystemPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 5);

        return constructor.newInstance(
                null,
                null,
                null,
                null,
                PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
        );
    }

    @Override
    protected Object getFakeUserPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 5);

        final var signingInfo = psPackageInfo != null
                ? psPackageInfo.signingInfo
                : null;

        return constructor.newInstance(
                VENDING_PACKAGE_NAME,
                signingInfo,
                VENDING_PACKAGE_NAME,
                VENDING_PACKAGE_NAME,
                PackageInstaller.PACKAGE_SOURCE_STORE
        );
    }
}
