package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PMS_COMPUTER_TRACKER_CLASS

@RequiresApi(Build.VERSION_CODES.S)
class PmsHookTarget31 : PmsHookTargetBase() {
    override val TAG = "PmsHookTarget31"

    override fun load() {
        logI(TAG, null) { "Load hook" }

        hooker.apply {
            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageSetting",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { Binder.getCallingUid() },
                    { getArgument(frame, 1) as? String },
                    ::getCallingApps,
                    null,
                )
            }

            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageSettingInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { getArgument(frame, 2) as? Int },
                    { getArgument(frame, 1) as? String },
                    ::getCallingApps,
                    null,
                )
            }

            hookAfter(
                PMS_COMPUTER_TRACKER_CLASS,
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
                PMS_COMPUTER_TRACKER_CLASS,
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

            hookBefore(
                APPS_FILTER_CLASS,
                "shouldFilterApplication",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { getArgument(frame, 1) as? Int },
                    { getPackageNameFromPackageSettings(getArgument(frame, 3)) },
                    ::getCallingApps,
                    true,
                )
            }
        }
    }
}
