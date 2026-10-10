package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;

import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM;
import static icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER;
import static icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME;

import android.os.Binder;

import java.lang.reflect.InvocationTargetException;

public class InstallerHookTarget29 extends InstallerHookTargetBase {

    public InstallerHookTarget29() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super("InstallerHookTarget29");
    }

    InstallerHookTarget29(String tag) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super(tag);
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                service.pms.getClass().getName(),
                "getInstallerPackageName",
                (methodName, frame, returnValue) -> {
                    final var result = applyInstallerHiding(methodName,
                            Binder.getCallingUid(), () -> (String) getArgument(frame, 1));

                    switch (result) {
                        case FAKE_INSTALLATION_SOURCE_USER -> returnValue.setResult(VENDING_PACKAGE_NAME);
                        case FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.setResult(null);
                    }
                }
        );
    }

    // not required until SDK 30
    @Override
    protected Object getFakeSystemPackageInstallSourceInfo() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return null;
    }

    // not required until SDK 30
    @Override
    protected Object getFakeUserPackageInstallSourceInfo() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return null;
    }
}
