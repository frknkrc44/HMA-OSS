package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageInstaller
import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastWithType
import icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_SYSTEM
import icu.nullptr.hidemyapplist.common.Constants.FAKE_INSTALLATION_SOURCE_USER
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
open class InstallerHookTarget33 : InstallerHookTargetBase() {
    override val TAG = "InstallerHookTarget33"

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            5,
        )!!.newInstance(
            null,
            null,
            null,
            null,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            5,
        )!!.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            PackageInstaller.PACKAGE_SOURCE_STORE,
        )
    }

    private val androidPkgClazzNames = arrayOf("AndroidPackage", "PackageImpl")

    override fun load() {
        super.load()

        hooker.apply {
            hookBefore(
                COMPUTER_ENGINE_CLASS,
                "isCallerInstallerOfRecord",
            ) { methodName, frame, returnValue ->
                val callingUid = frame.args.lastWithType<Int>()

                applyInstallerHiding(
                    methodName,
                    callingUid,
                    fta@{
                        val pkg = frame.args.lastOrNull {
                            it?.javaClass?.simpleName in androidPkgClazzNames
                        } ?: return@fta null
                        callMethod(pkg,
                            if (pkg.javaClass.simpleName == "PackageImpl") {
                                "getManifestPackageName"
                            } else {
                                "getPackageName"
                            }
                        ) as? String
                    }
                ) {
                    when (it) {
                        FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = callingUid == psPackageInfo?.applicationInfo?.uid
                        FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = false
                    }
                }
            }

            hookBefore(
                COMPUTER_ENGINE_CLASS,
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

            hookBefore(
                COMPUTER_ENGINE_CLASS,
                "getInstallerPackageName",
            ) { methodName, frame, returnValue ->
                applyInstallerHiding(
                    methodName,
                    Binder.getCallingUid(),
                    { frame.args.firstOrNullWithType() },
                ) {
                    when (it) {
                        FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = VENDING_PACKAGE_NAME
                        FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = null
                    }
                }
            }
        }
    }
}
