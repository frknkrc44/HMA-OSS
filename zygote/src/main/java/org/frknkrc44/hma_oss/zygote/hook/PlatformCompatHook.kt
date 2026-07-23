package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.annotation.RequiresApi
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.sAppDataIsolationEnabled
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PLATFORM_COMPAT_CLASS

@RequiresApi(Build.VERSION_CODES.R)
class PlatformCompatHook : IFrameworkHook {
    override val TAG = "PlatformCompatHook"

    override fun load() {
        logI(TAG) { "Load hook" }
        logI(TAG) { "App data isolation enabled: $sAppDataIsolationEnabled" }

        BulkHooker.instance.hookBefore(
            PLATFORM_COMPAT_CLASS,
            "isChangeEnabled",
        ) { _, frame, returnValue ->
            if (service?.config?.forceMountData ?: false) return@hookBefore

            runCatching {
                if (!sAppDataIsolationEnabled) return@hookBefore

                val changeId = frame.getArgument(1) as Long
                if (changeId != 143937733L) return@hookBefore

                val appInfo = frame.getArgument(2) as? ApplicationInfo ?: return@hookBefore
                val app = appInfo.packageName
                if (app == BuildConfig.APP_PACKAGE_NAME) return@hookBefore
                if (service?.isHookEnabled(app) ?: false) {
                    returnValue.result = true
                    logD(TAG) { "force mount data: ${appInfo.uid} $app" }
                }
            }.onFailure {
                logE(TAG, it) { "Fatal error occurred, disable hooks" }
            }
        }
    }
}
