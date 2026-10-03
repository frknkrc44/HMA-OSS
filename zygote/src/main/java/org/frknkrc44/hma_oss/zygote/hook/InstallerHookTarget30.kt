package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM
import icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument

@RequiresApi(Build.VERSION_CODES.R)
class InstallerHookTarget30 : InstallerHookTarget29() {
    override val TAG = "InstallerHookTarget30"

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            4,
        )!!.newInstance(
            null,
            null,
            null,
            null,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            4,
        )!!.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
        )
    }

    override fun load() {
        super.load()

        hooker.apply {
            hookBefore(
                service.pms.javaClass.name,
                "getInstallSourceInfo",
            ) { methodName, frame, returnValue ->
                applyInstallerHiding(
                    methodName,
                    Binder.getCallingUid(),
                    { frame.getArgument(1) as? String }
                ) {
                    when (it) {
                        FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = fakeUserPackageInstallSourceInfo
                        FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = fakeSystemPackageInstallSourceInfo
                    }
                }
            }
        }
    }
}
