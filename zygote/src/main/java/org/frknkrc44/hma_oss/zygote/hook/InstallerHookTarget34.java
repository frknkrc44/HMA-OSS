package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.INSTALL_SOURCE_INFO_CLASS;
import static icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME;

import android.content.pm.PackageInstaller;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class InstallerHookTarget34 extends InstallerHookTarget33 {

    public InstallerHookTarget34() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        super("InstallerHookTarget34");
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected Object getFakeSystemPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 6);

        return constructor.newInstance(
                null,
                null,
                null,
                null,
                null,
                PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected Object getFakeUserPackageInstallSourceInfo()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = findConstructor(INSTALL_SOURCE_INFO_CLASS, 6);

        final var signingInfo = psPackageInfo != null
                ? psPackageInfo.signingInfo
                : null;

        return constructor.newInstance(
                VENDING_PACKAGE_NAME,
                signingInfo,
                VENDING_PACKAGE_NAME,
                VENDING_PACKAGE_NAME,
                VENDING_PACKAGE_NAME,
                PackageInstaller.PACKAGE_SOURCE_STORE
        );
    }
}
