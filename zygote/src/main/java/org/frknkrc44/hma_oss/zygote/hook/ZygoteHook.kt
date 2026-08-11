package org.frknkrc44.hma_oss.zygote.hook

import android.os.Build
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.sAppDataIsolationEnabled
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.argTypes
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.NATIVE_ZYGOTE_PROCESS_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS
import java.util.concurrent.atomic.AtomicReference

class ZygoteHook : IFrameworkHook {
    override val TAG = "ZygoteHook"

    private val lastForceMountedApp: AtomicReference<String?> = AtomicReference(null)

    private val forceMountData get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            service?.config?.forceMountData ?: false &&
            sAppDataIsolationEnabled

    override fun load() {
        BulkHooker.instance.apply {
            hookBefore(
                ZYGOTE_PROCESS_CLASS,
                "start",
                hook = this@ZygoteHook::hookIntoZygoteProcess,
            )

            // TODO: Replace with variable later
            if (Build.VERSION.SDK_INT >= 37) {
                hookBefore(
                    NATIVE_ZYGOTE_PROCESS_CLASS,
                    "start",
                    hook = this@ZygoteHook::hookIntoZygoteProcess,
                )
            }
        }
    }

    @Suppress("unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun hookIntoZygoteProcess(
        methodName: String,
        frame: EmulatedStackFrame,
        returnValue: ReturnValue,
    ) {
        logD(TAG) { "@startZygoteProcess: Starting ${frame.args.contentToString()}" }

        val caller = frame.args.lastOrNullWithType<String>() ?: return
        val isHookEnabled = service?.isHookEnabled(caller) ?: false
        if (!isHookEnabled) return

        // another plan for PlatformCompatHook
        if (forceMountData && !(service?.systemApps?.contains(caller) ?: false)) {
            val lastMapIndex = frame.argTypes.indexOfLast {
                it.isAssignableFrom(java.util.Map::class.java)
            }
            if (lastMapIndex >= 0) {
                // enable bindMountAppsData after checks
                val bindMountAppsDataIndex = lastMapIndex + 1
                if (frame.accessor().getArgumentShorty(bindMountAppsDataIndex) == 'Z') {
                    val last = lastForceMountedApp.getAndSet(caller)
                    if (last != caller) logI(TAG) { "@startZygoteProcess: force mountAppsData for $caller" }
                    frame.setArgument(bindMountAppsDataIndex, true)
                }
            }
        }

        // ignore if the GIDs array is null
        val gIDsIndex = frame.args.indexOfFirst { it is IntArray }
        if (gIDsIndex < 0) return

        var perms = service?.getRestrictedZygotePermissions(caller) ?: return
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
