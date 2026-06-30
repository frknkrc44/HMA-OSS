package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_IMPL_CLASS

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PmsHookTarget33 : PmsHookTargetBase() {

    override val TAG = "PmsHookTarget33"

    private val getPackagesForUidMethod by lazy {
        findMethod(
            "com.android.server.pm.Computer",
            "getPackagesForUid",
            isDeclared = false,
            systemClassLoader = true,
            Int::class.java,
        )
    }

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

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                APPS_FILTER_IMPL_CLASS,
                "shouldFilterApplication",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArgument(2) as Int? },
                    { getPackageNameFromPackageSettings(frame.getArgument(4)) },
                    {
                        Utils.binderLocalScope {
                            getPackagesForUidMethod.invoke(frame.getArgument(1), it) as Array<String>?
                        }
                    },
                    { returnValue.result = true },
                )
            }
        }

        super.load()
    }
}
