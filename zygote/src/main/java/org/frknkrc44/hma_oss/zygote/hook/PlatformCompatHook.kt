package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.PropertyUtils
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArg
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PLATFORM_COMPAT_CLASS

@RequiresApi(Build.VERSION_CODES.R)
class PlatformCompatHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "PlatformCompatHook"

    private val sAppDataIsolationEnabled by lazy {
        PropertyUtils.isAppDataIsolationEnabled || service.config.altAppDataIsolation
    }

    override fun load() {
        if (!service.config.forceMountData) return
        logI(TAG) { "Load hook" }
        logI(TAG) { "App data isolation enabled: $sAppDataIsolationEnabled" }

        BulkHooker.instance.hookBefore(
            PLATFORM_COMPAT_CLASS,
            "isChangeEnabled",
        ) { _, frame, returnValue ->
            runCatching {
                if (!sAppDataIsolationEnabled) return@hookBefore

                val changeId = frame.getArg(1) as Long
                if (changeId != 143937733L) return@hookBefore

                val appInfo = frame.getArg(2) as ApplicationInfo
                val app = appInfo.packageName
                if (app == BuildConfig.APP_PACKAGE_NAME || app in service.systemApps) return@hookBefore
                if (service.isHookEnabled(app)) {
                    returnValue.result = true
                    logD(TAG) { "force mount data: ${appInfo.uid} $app" }
                }
            }.onFailure {
                logE(TAG, it) { "Fatal error occurred, disable hooks" }
            }
        }
    }

    override fun onConfigChanged() {
        /*
        if (service.config.forceMountData) {
            if (hook == null) load()
        } else {
            if (hook != null) unload()
        }
         */
    }
}
