package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_DISABLED;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER;
import static icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME;
import static icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat;
import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.function.Supplier;

import icu.nullptr.hidemyapplist.common.Constants;

public abstract class InstallerHookTargetBase extends ABaseFrameworkHook {

    protected Object fakeSystemPackageInstallSourceInfo;
    protected Object fakeUserPackageInstallSourceInfo;

    InstallerHookTargetBase(String tag) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super(tag);

        try {
            psPackageInfo = getPackageInfoCompat(
                    service.pms,
                    VENDING_PACKAGE_NAME,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                    0
            );
        } catch (Throwable ignore) {}

        fakeSystemPackageInstallSourceInfo = getFakeSystemPackageInstallSourceInfo();
        fakeUserPackageInstallSourceInfo = getFakeUserPackageInstallSourceInfo();
    }

    protected PackageInfo psPackageInfo;

    @Override
    public void load() {
        super.load();

        if (service.pmn != null) {
            service.hooker.hookBefore(
                    service.pmn.getClass().getName(),
                    "getInstallerForPackage",
                    (methodName, frame, returnValue) -> {
                        final var result = applyInstallerHiding(methodName,
                                Binder.getCallingUid(), () -> (String) getArgument(frame, 1));

                        switch (result) {
                            case FAKE_INSTALLATION_SOURCE_USER -> returnValue.setResult(VENDING_PACKAGE_NAME);
                            case FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.setResult("preload");
                        }
                    }
            );
        }
    }

    protected int applyInstallerHiding(String methodName, int callingUid, Supplier<String> findTargetApp) throws RemoteException {
        if (callingUid == Constants.UID_SYSTEM) {
            return FAKE_INSTALLATION_SOURCE_DISABLED;
        }

        final var query = findTargetApp.get();
        if (query == null) {
            return FAKE_INSTALLATION_SOURCE_DISABLED;
        }

        final var callingApps = getCallingApps(service.pms, callingUid);
        final var callingUser = getUserFromCallingUid(callingUid);

        var hide = FAKE_INSTALLATION_SOURCE_DISABLED;
        for (var caller : callingApps) {
            hide = service.shouldHideInstallationSource(caller, query, callingUser);
            if (hide == FAKE_INSTALLATION_SOURCE_DISABLED) {
                continue;
            }

            final var finalHide = hide;
            logD(TAG, null, () -> String.format(Locale.US,
                    "@%s: Applied installer hiding for %s - %d -> %d",
                    methodName, caller, callingUid, finalHide
            ));

            service.increaseInstallerFilterCount(caller);
            break;
        }

        return hide;
    }

    protected abstract Object getFakeSystemPackageInstallSourceInfo() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException;
    protected abstract Object getFakeUserPackageInstallSourceInfo() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException;
}
