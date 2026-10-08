package org.frknkrc44.hma_oss.zygote.hook

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.PropertyUtils
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.SystemServerHook
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getBooleanField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getThisObject
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setBooleanField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_LIST_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_RECORD_INTERNAL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.STORAGE_MANAGER_SERVICE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.STORAGE_MANAGER_SERVICE_LIFECYCLE_CLASS
import java.util.Map

@SuppressLint("PrivateApi")
@RequiresApi(Build.VERSION_CODES.R)
class AppDataIsolationHook : IFrameworkHook {
    override val TAG = "AppDataIsolationHook"

    companion object {
        private const val APPDATA_ISOLATION_ENABLED = "mAppDataIsolationEnabled"
        private const val VOLD_APPDATA_ISOLATION_ENABLED = "mVoldAppDataIsolationEnabled"
        private const val FUSE_PROP = "persist.sys.fuse"
    }

    private var voldHookSkipped = false

    private val processRecordIntClass: Class<*> by lazy {
        Class.forName(
            PROCESS_RECORD_INTERNAL_CLASS,
            true,
            SystemServerHook.classLoader,
        )
    }

    private val isAltIsolationEnabled get() = config.let {
        (!PropertyUtils.isAppDataIsolationEnabled && it.altAppDataIsolation) ||
        (!PropertyUtils.isVoldAppDataIsolationEnabled && it.altVoldAppDataIsolation)
    }

    @SuppressLint("PrivateApi")
    override fun load() {
        if (!isAltIsolationEnabled) return
        logI(TAG, null) { "Load hook" }

        hooker.apply {
            hookBefore(
                PROCESS_LIST_CLASS,
                "startProcess",
            ) { _, frame, _ ->
                val processListClazz = runCatching {
                    Class.forName(PROCESS_LIST_CLASS, true, SystemServerHook.classLoader)
                }.getOrNull()

                if (config.altAppDataIsolation) {
                    val thisObject = getThisObject(frame)

                    val isEnabled = getBooleanField(
                        thisObject,
                        APPDATA_ISOLATION_ENABLED,
                        processListClazz,
                    )

                    if (!isEnabled) {
                        setBooleanField(
                            thisObject,
                            APPDATA_ISOLATION_ENABLED,
                            true,
                            processListClazz,
                        )

                        logI(TAG, null) { "ProcessList - App data isolation is forced" }
                    }
                }

                if (config.altVoldAppDataIsolation && !voldHookSkipped) {
                    val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                    if (!fuseEnabled) {
                        voldHookSkipped = true
                        logE(TAG, null) { "ProcessList - FUSE storage is not enabled, skip vold hook" }
                    } else {
                        val thisObject = getThisObject(frame)

                        val isolationEnabled = getBooleanField(
                            thisObject,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            processListClazz,
                        )

                        if (!isolationEnabled) {
                            setBooleanField(
                                thisObject,
                                VOLD_APPDATA_ISOLATION_ENABLED,
                                true,
                                processListClazz,
                            )

                            logI(TAG, null) { "ProcessList - Vold app data isolation is forced" }
                        }
                    }
                }
            }

            if (OSUtils.isSamsung()) {
                hookAfter(
                    STORAGE_MANAGER_SERVICE_LIFECYCLE_CLASS,
                    "onStart"
                ) { _, frame, _ ->
                    if (config.altVoldAppDataIsolation && !voldHookSkipped) {
                        val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                        if (!fuseEnabled) {
                            logE(TAG, null) { "StorageManagerService - FUSE storage is not enabled, skip vold hook" }
                            voldHookSkipped = true
                            return@hookAfter
                        }

                        val thisObject = getThisObject(frame)
                        val storageManagerService = getObjectField(
                            thisObject,
                            "mStorageManagerService",
                        )!!

                        val isolationEnabled = getBooleanField(
                            storageManagerService,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            null,
                        )

                        if (!isolationEnabled) {
                            setBooleanField(
                                storageManagerService,
                                VOLD_APPDATA_ISOLATION_ENABLED,
                                true,
                                null,
                            )

                            logI(TAG, null) { "StorageManagerService - Vold app data isolation is forced" }
                        }
                    }
                }
            } else {
                hookBefore(
                    STORAGE_MANAGER_SERVICE_CLASS,
                    "onVolumeStateChangedLocked",
                ) { _, frame, _ ->
                    if (config.altVoldAppDataIsolation && !voldHookSkipped) {
                        val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                        if (!fuseEnabled) {
                            logE(TAG, null) { "StorageManagerService - FUSE storage is not enabled, skip vold hook" }
                            voldHookSkipped = true
                            return@hookBefore
                        }

                        val storageManagerService = getThisObject(frame)
                        val isolationEnabled = getBooleanField(
                            storageManagerService,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            null,
                        )

                        if (!isolationEnabled) {
                            setBooleanField(
                                storageManagerService,
                                VOLD_APPDATA_ISOLATION_ENABLED,
                                true,
                                null,
                            )

                            logI(TAG, null) { "StorageManagerService - Vold app data isolation is forced" }
                        }
                    }
                }

                hookAfter(
                    PROCESS_LIST_CLASS,
                    "needsStorageDataIsolation",
                ) { _, frame, returnValue ->
                    if (config.altVoldAppDataIsolation) {
                        val args = dumpArgs(frame, true)
                        val app = args.find { it?.javaClass?.simpleName == "ProcessRecord" }!!
                        val uid = runCatching {
                            getIntField(app, "uid")
                        }.getOrElse {
                            getIntField(app, "uid", processRecordIntClass)
                        }

                        val apps = getCallingApps(pms, uid)

                        if (config.detailLog) {
                            val processName = runCatching {
                                getObjectField(app, "processName")
                            }.getOrElse {
                                getObjectField(app, "processName", processRecordIntClass)
                            }
                            val mountNode = runCatching {
                                getIntField(app, "mMountMode")
                            }.getOrDefault(0)
                            val isolated = runCatching {
                                getBooleanField(app, "isolated", null)
                            }.getOrElse {
                                getBooleanField(app, "isolated", processRecordIntClass)
                            }
                            val appZygote = runCatching {
                                getBooleanField(app, "appZygote", null)
                            }.getOrElse {
                                getBooleanField(app, "appZygote", processRecordIntClass)
                            }

                            logD(TAG, null) { "@needsStorageDataIsolation $uid and ${apps.contentToString()} - $processName value without override: ${returnValue.result}, mount node: $mountNode, isolated: $isolated, appZygote: $appZygote" }
                        }

                        // Do not isolate this module for safety
                        if (apps.contains(BuildConfig.APP_PACKAGE_NAME)) {
                            returnValue.result = false
                            return@hookAfter
                        }

                        if (apps.any { service.isAppDataIsolationExcluded(it) }) {
                            returnValue.result = false
                            return@hookAfter
                        }

                        if (config.skipSystemAppDataIsolation) {
                            val isSystemApp = systemApps.any { apps.contains(it) }
                            logD(TAG, null) { "@needsStorageDataIsolation $uid and ${apps.contentToString()} - isSystemApp: $isSystemApp" }

                            if (isSystemApp) {
                                returnValue.result = false
                                return@hookAfter
                            }
                        }
                    }
                }

                hookBefore(
                    STORAGE_MANAGER_SERVICE_CLASS,
                    "remountAppStorageDirs",
                ) { _, frame, _ ->
                    if (!voldHookSkipped && config.altVoldAppDataIsolation && config.skipSystemAppDataIsolation) {
                        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                        val pidPkgMap = getArgument(frame, 1) as Map<*, *>
                        val keysToRemove = mutableSetOf<Any>()

                        for (entry in pidPkgMap.entrySet()) {
                            val pid = entry.key
                            val packageName = entry.value as String

                            if (packageName in systemApps || packageName == BuildConfig.APP_PACKAGE_NAME) {
                                logD(TAG, null) { "@remountAppStorageDirs SYSTEM $pid - $packageName is marked to remove" }
                                keysToRemove += pid
                                break
                            }
                        }

                        keysToRemove.forEach { pidPkgMap.remove(it) }
                    }
                }
            }
        }
    }
}
