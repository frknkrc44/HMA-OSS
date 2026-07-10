package org.frknkrc44.hma_oss.zygote.hook

import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS

class ZygoteHook : IFrameworkHook {
    override val TAG = "ZygoteHook"

    private val forceMountData get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            service?.config?.forceMountData ?: false

    override fun load() {
        BulkHooker.instance.hookBefore(
            ZYGOTE_PROCESS_CLASS,
            "start",
        ) { _, frame, _ ->
            logD(TAG) { "@startZygoteProcess: Starting ${frame.args.contentToString()}" }

            val caller = frame.args.lastOrNullWithType<String>() ?: return@hookBefore
            val isHookEnabled = service?.isHookEnabled(caller) ?: false
            if (!isHookEnabled) return@hookBefore

            // ignore if the GIDs array is null
            val gIDsIndex = frame.args.indexOfFirst { it is IntArray }
            if (gIDsIndex < 0) return@hookBefore

            // another plan for PlatformCompatHook
            if (forceMountData && !(service?.systemApps?.contains(caller) ?: false)) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                val lastMapIndex = frame.args.indexOfLast { it is java.util.Map<*, *> }
                if (lastMapIndex >= 0) {
                    // enable bindMountAppsData after checks
                    val bindMountAppsDataIndex = lastMapIndex + 1
                    if (frame.accessor().getArgumentShorty(bindMountAppsDataIndex) == 'Z') {
                        logD(TAG) { "@startZygoteProcess: Replacing bindMountAppsData flag" }
                        frame.setArgument(bindMountAppsDataIndex, true)
                    }
                }
            }

            var perms = service?.getRestrictedZygotePermissions(caller) ?: return@hookBefore
            if (perms.isNotEmpty()) {
                val gIDs = frame.args[gIDsIndex] as IntArray

                // add more security, reject if not available in GID_PAIRS
                perms = perms.filter { Constants.GID_PAIRS.containsValue(it) }

                logD(TAG) { "@startZygoteProcess: GIDs are ${gIDs.contentToString()}, removing $perms now" }
                frame.setArgument(gIDsIndex, gIDs.filter { it !in perms }.toIntArray())
                service?.increaseOthersFilterCount(caller)
            }
        }
    }
}
