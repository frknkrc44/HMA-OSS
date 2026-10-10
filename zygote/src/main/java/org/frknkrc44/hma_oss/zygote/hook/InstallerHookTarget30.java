package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.INSTALL_SOURCE_INFO_CLASS;

import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER;
import static icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME;

import android.os.Binder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("DataFlowIssue")
@RequiresApi(Build.VERSION_CODES.R)
public class InstallerHookTarget30 extends InstallerHookTarget29 {

    public InstallerHookTarget30() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super("InstallerHookTarget30");
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                service.pms.getClass().getName(),
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
    }

    @Override
    protected Object getFakeSystemPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 4);

        return constructor.newInstance(
                null,
                null,
                null,
                null
        );
    }

    @Override
    protected Object getFakeUserPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 4);

        final var signingInfo = psPackageInfo != null
                ? psPackageInfo.signingInfo
                : null;

        return constructor.newInstance(
                VENDING_PACKAGE_NAME,
                signingInfo,
                VENDING_PACKAGE_NAME,
                VENDING_PACKAGE_NAME
        );
    }
}
