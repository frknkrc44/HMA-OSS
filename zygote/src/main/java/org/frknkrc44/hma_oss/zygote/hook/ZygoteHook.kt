package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.ServiceInfo
import android.os.Build
import android.os.ProcessParams
import com.v7878.unsafe.ArtMethodUtils
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.isAppDataIsolationEnabled
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.argTypes
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
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
            config.forceMountData &&
            isAppDataIsolationEnabled(config)

    override fun load() {
        hooker.apply {
            hookBefore(
                ZYGOTE_PROCESS_CLASS,
                "start",
            ) { _, frame, _ ->
                hookIntoZygoteProcess(frame)
            }

            // TODO: Try to find a way for Android 12- compatibility without harming TANGO support
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Try to fix PrivIsolated
                hookBefore(
                    SERVICE_RECORD_CLASS,
                    CONSTRUCTOR_METHOD_NAME,
                ) { _, frame, _ ->
                    val caller = frame.args.firstOrNullWithType<String>() ?: return@hookBefore
                    val perms = service.getRestrictedZygotePermissions(caller) ?: return@hookBefore
                    if (!perms.contains(Constants.APP_ZYGOTE_GID)) return@hookBefore

                    val serviceInfo = frame.args.firstOrNullWithType<ServiceInfo>() ?: return@hookBefore
                    if (serviceInfo.flags and ServiceInfo.FLAG_ISOLATED_PROCESS == 0) return@hookBefore
                    if (serviceInfo.flags and ServiceInfo.FLAG_NATIVE_SERVICE != 0) return@hookBefore

                    logD(TAG) { "@serviceRecord: Isolated process becomes app zygote process for $caller service" }
                    serviceInfo.flags = serviceInfo.flags or ServiceInfo.FLAG_USE_APP_ZYGOTE
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                hookBefore(
                    NATIVE_ZYGOTE_PROCESS_CLASS,
                    "start",
                ) { _, frame, _ ->
                    hookIntoZygoteProcess(frame)
                }
            }
        }
    }

    private fun hookIntoZygoteProcess(frame: EmulatedStackFrame) {
        if (frame.type().parameterCount() < 3) {
            hookIntoZygoteProcessModern(frame)
        } else {
            hookIntoZygoteProcessLegacy(frame)
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun hookIntoZygoteProcessLegacy(frame: EmulatedStackFrame) {
        logD(TAG) { "@startZygoteProcessLegacy: Starting ${frame.args.contentToString()}" }

        val caller = frame.args.lastOrNullWithType<String>() ?: return
        val isHookEnabled = service.isHookEnabled(caller)
        if (!isHookEnabled) return

        // another plan for PlatformCompatHook
        val pair = getForceMountArgs(frame, caller)
        if (pair.first) {
            val lastMapIndex = frame.argTypes.indexOfLast {
                it == java.util.Map::class.java
            }
            if (lastMapIndex >= 0) {
                // enable bindMountAppsData after checks
                val bindMountAppsDataIndex = lastMapIndex + 1
                if (frame.shortyEquals(bindMountAppsDataIndex, 'Z')) {
                    val last = lastForceMountedApp.getAndSet(caller)
                    if (last != caller) logI(TAG) { "@startZygoteProcessLegacy: force mountAppsData for $caller" }
                    frame.setArgument(bindMountAppsDataIndex, true)
                    logD(TAG) { "@startZygoteProcessLegacy: mountAppsData argument overridden for $caller" }
                }
            }
        }

        if (pair.second < 0) return

        var perms = service.getRestrictedZygotePermissions(caller) ?: return
        if (perms.isNotEmpty()) {
            perms = perms.filter {
                // reject if not available in GID_PAIRS, or it is APP_ZYGOTE_GID
                Constants.GID_PAIRS.containsValue(it) || it == Constants.APP_ZYGOTE_GID
            }
            if (perms.isEmpty()) return

            val gIDs = frame.args[pair.second] as? IntArray ?: return

            logD(TAG) { "@startZygoteProcessLegacy: GIDs are ${gIDs.contentToString()}, removing $perms now" }
            frame.setArgument(pair.second, gIDs.filter { it !in perms }.toIntArray())
            service.increaseOthersFilterCount(caller)
        }
    }

    /**
     * This method is added on Android 17 QPR3 Beta 1
     */
    private fun hookIntoZygoteProcessModern(frame: EmulatedStackFrame) {
        logD(TAG) { "@startZygoteProcessModern: Starting ${frame.args.contentToString()}" }
        val processParams = frame.getArgument(1) as ProcessParams

        val caller = processParams.packageName
        val isHookEnabled = service.isHookEnabled(caller)
        if (!isHookEnabled) return

        fun makeProcessParamsBuilder(processParams: ProcessParams): ProcessParams.Builder {
            val constructor = ProcessParams.Builder::class.java.getDeclaredConstructor(
                ProcessParams::class.java
            )
            ArtMethodUtils.makeExecutablePublic(constructor)
            return constructor.newInstance(processParams) as ProcessParams.Builder
        }

        var builder: ProcessParams.Builder? = null
        if (processParams.targetSdkVersion < Build.VERSION_CODES.R) {
            builder = makeProcessParamsBuilder(processParams)
            builder.setBindMountAppsData(true)
        }

        fun runFinish() {
            builder?.let { frame.setArgument(1, it.build()) }
        }

        if (processParams.gids == null) {
            return runFinish()
        }

        var perms = service.getRestrictedZygotePermissions(caller) ?: return runFinish()
        if (perms.isNotEmpty()) {
            perms = perms.filter {
                // reject if not available in GID_PAIRS, or it is APP_ZYGOTE_GID
                Constants.GID_PAIRS.containsValue(it) || it == Constants.APP_ZYGOTE_GID
            }
            if (perms.isEmpty()) return runFinish()

            if (builder == null) builder = makeProcessParamsBuilder(processParams)
            val gIDs = processParams.gids

            logD(TAG) { "@startZygoteProcessModern: GIDs are ${gIDs.contentToString()}, removing $perms now" }

            builder.setGids(gIDs.filter { it !in perms }.toIntArray())
            service.increaseOthersFilterCount(caller)
        }

        runFinish()
    }

    private fun getForceMountArgs(frame: EmulatedStackFrame, caller: String): Pair<Boolean, Int> {
        var gIDsVarIndex = -1
        for ((i, clazz) in frame.argTypes.withIndex()) {
            if (clazz == IntArray::class.java) {
                gIDsVarIndex = i
                continue
            }

            if (gIDsVarIndex < 0) continue

            if (!forceMountData || systemApps.contains(caller)) {
                return Pair(false, gIDsVarIndex)
            }

            if (clazz == String::class.java) {
                val targetSDKVar = frame.args[i - 1]
                if (targetSDKVar is Int && targetSDKVar >= 30) {
                    return Pair(false, gIDsVarIndex)
                }
            }

            if (clazz == LongArray::class.java) {
                val isTopAppIndex = i - 1
                return Pair(frame.args[isTopAppIndex] == true, gIDsVarIndex)
            }
        }

        return Pair(false, gIDsVarIndex)
    }
}
