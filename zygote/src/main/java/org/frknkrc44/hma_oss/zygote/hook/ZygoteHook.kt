package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.ServiceInfo
import android.os.Build
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.sAppDataIsolationEnabled
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.argTypes
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.shortyEquals
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONSTRUCTOR_METHOD_NAME
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.NATIVE_ZYGOTE_PROCESS_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.SERVICE_RECORD_CLASS
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
                "startViaZygote",
            ) { _, frame, _ ->
                val packageNameIndex = frame.args.indexOfLast { it is String }
                if (packageNameIndex < 0) return@hookBefore

                val isChildZygoteIndex = packageNameIndex - 1
                if (frame.shortyEquals(isChildZygoteIndex, 'Z')) {
                    val isChildZygote = frame.args[isChildZygoteIndex] == true

                    hookIntoZygoteProcess(frame, isChildZygote)
                }
            }

            if (!isHookAvailable(ZYGOTE_PROCESS_CLASS, "startViaZygote")) {
                hookBefore(
                    ZYGOTE_PROCESS_CLASS,
                    "start",
                ) { _, frame, _ ->
                    hookIntoZygoteProcess(frame, false)
                }
            }

            // Try to fix PrivIsolated
            hookBefore(
                SERVICE_RECORD_CLASS,
                CONSTRUCTOR_METHOD_NAME,
            ) { _, frame, _ ->
                val caller = frame.args.firstOrNullWithType<String>() ?: return@hookBefore
                val perms = service?.getRestrictedZygotePermissions(caller) ?: return@hookBefore
                if (!perms.contains(Constants.APP_ZYGOTE_GID)) return@hookBefore

                val serviceInfo = frame.args.firstOrNullWithType<ServiceInfo>() ?: return@hookBefore
                if (serviceInfo.flags and ServiceInfo.FLAG_ISOLATED_PROCESS == 0) return@hookBefore

                logD(TAG) { "@serviceRecord: Isolated process becomes app zygote process for $caller service" }
                serviceInfo.flags = serviceInfo.flags or ServiceInfo.FLAG_USE_APP_ZYGOTE
            }

            // TODO: Replace with variable later
            if (Build.VERSION.SDK_INT >= 37) {
                hookBefore(
                    NATIVE_ZYGOTE_PROCESS_CLASS,
                    "start",
                ) { _, frame, _ ->
                    hookIntoZygoteProcess(frame, false)
                }
            }
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun hookIntoZygoteProcess(
        frame: EmulatedStackFrame,
        isChildZygote: Boolean,
    ) {
        logD(TAG) { "@startZygoteProcess: Starting ${frame.args.contentToString()}" }

        val caller = frame.args.lastOrNullWithType<String>() ?: return
        val isHookEnabled = service?.isHookEnabled(caller) ?: false
        if (!isHookEnabled) return

        // another plan for PlatformCompatHook
        if (!isChildZygote && isZygoteProcessForceMounted(frame, caller)) {
            val lastMapIndex = frame.argTypes.indexOfLast {
                it.isAssignableFrom(java.util.Map::class.java)
            }
            if (lastMapIndex >= 0) {
                // enable bindMountAppsData after checks
                val bindMountAppsDataIndex = lastMapIndex + 1
                if (frame.shortyEquals(bindMountAppsDataIndex, 'Z')) {
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

    fun isZygoteProcessForceMounted(frame: EmulatedStackFrame, caller: String): Boolean {
        if (!forceMountData || (service?.systemApps?.contains(caller) ?: false)) return false

        val longArrayIndex = frame.argTypes.indexOfFirst {
            it.isAssignableFrom(LongArray::class.java)
        }
        if (longArrayIndex < 0) return false

        val isTopAppIndex = longArrayIndex - 1
        return frame.args[isTopAppIndex] == true
    }
}
