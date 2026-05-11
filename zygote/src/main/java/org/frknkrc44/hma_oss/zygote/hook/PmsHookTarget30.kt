package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

@RequiresApi(Build.VERSION_CODES.R)
class PmsHookTarget30(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget30"

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
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getPackageSetting",
            ) { param ->
                applyPackageHiding(
                    param.methodName,
                    { Binder.getCallingUid() },
                    { param.getArgument(1) as String? },
                    { getCallingApps(service, it) },
                    { param.result = null },
                )
            }

            hookBefore(
                APPS_FILTER_CLASS,
                "shouldFilterApplication",
            ) { param ->
                applyPackageHiding(
                    param.methodName,
                    { param.getArgument(1) as Int },
                    { getPackageNameFromPackageSettings(param.getArgument(3)) },
                    { getCallingApps(service, it) },
                    { param.result = true },
                )
            }

            hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getPackageInfoInternal",
            ) { param ->
                applyPackageHiding(
                    param.methodName,
                    { param.getArgument(4) as? Int },
                    { param.getArgument(1) as? String },
                    { getCallingApps(service, it) },
                    { param.result = null },
                )
            }

            hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getApplicationInfoInternal",
            ) { param ->
                applyPackageHiding(
                    param.methodName,
                    { param.getArgument(3) as? Int },
                    { param.getArgument(1) as? String },
                    { getCallingApps(service, it) },
                    { param.result = null },
                )
            }
        }

        super.load()
    }
}
