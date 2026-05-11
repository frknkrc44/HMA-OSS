package org.frknkrc44.hma_oss.zygote.hook

import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

class PmsHookTarget29(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget29"

    // not required until SDK 30
    override val fakeSystemPackageInstallSourceInfo = null
    override val fakeUserPackageInstallSourceInfo = null

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                service.pms::class.java.name,
                "filterAppAccessLPr",
                paramCount = 5,
            ) { param ->
                applyPackageHiding(
                    param.methodName,
                    { param.getArgument(2) as Int? },
                    { getPackageNameFromPackageSettings(param.getArgument(1)) },
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
                    { param.getArgument(4) as Int? },
                    { param.getArgument(1) as String? },
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
                    { param.getArgument(3) as Int? },
                    { param.getArgument(1) as String? },
                    { getCallingApps(service, it) },
                    { param.result = null },
                )
            }
        }

        super.load()
    }
}
