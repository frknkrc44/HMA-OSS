package org.frknkrc44.hma_oss.zygote.hook

import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

class PmsHookTarget29 : PmsHookTargetBase() {
    override val TAG = "PmsHookTarget29"

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, null) { "Load hook" }

        hooker.apply {
            hookBefore(
                service.pms::class.java.name,
                "filterAppAccessLPr",
                5,
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { getArgument(frame, 2) as? Int },
                    { getPackageNameFromPackageSettings(getArgument(frame, 1)) },
                    ::getCallingApps,
                    true,
                )
            }

            hookAfter(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getPackageInfoInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { getArgument(frame, 4) as? Int },
                    { getArgument(frame, 1) as? String },
                    ::getCallingApps,
                    null,
                )
            }

            hookAfter(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getApplicationInfoInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { getArgument(frame, 3) as? Int },
                    { getArgument(frame, 1) as? String },
                    ::getCallingApps,
                    null,
                )
            }
        }
    }
}
