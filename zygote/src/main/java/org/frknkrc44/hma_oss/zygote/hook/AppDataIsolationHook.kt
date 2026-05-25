package org.frknkrc44.hma_oss.zygote.hook

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.RequiresApi
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.service.SystemServerHook
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getBooleanField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setBooleanField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_LIST_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PROCESS_RECORD_INTERNAL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.STORAGE_MANAGER_SERVICE_CLASS
import java.util.Map

@SuppressLint("PrivateApi")
@RequiresApi(Build.VERSION_CODES.R)
class AppDataIsolationHook(private val service: HMAService): IFrameworkHook {
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

    @SuppressLint("PrivateApi")
    override fun load() {
        if (!(service.config.altAppDataIsolation || service.config.altVoldAppDataIsolation)) return
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                PROCESS_LIST_CLASS,
                "startProcess",
            ) { param ->
                val processListClazz = runCatching {
                    Class.forName(PROCESS_LIST_CLASS, true, SystemServerHook.classLoader)
                }.getOrNull()

                if (service.config.altAppDataIsolation) {
                    val isEnabled = getBooleanField(
                        param.thisObject,
                        APPDATA_ISOLATION_ENABLED,
                        processListClazz,
                    )

                    if (!isEnabled) {
                        setBooleanField(
                            param.thisObject,
                            APPDATA_ISOLATION_ENABLED,
                            true,
                            processListClazz,
                        )

                        logI(TAG) { "ProcessList - App data isolation is forced" }
                    }
                }

                if (service.config.altVoldAppDataIsolation && !voldHookSkipped) {
                    val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                    if (!fuseEnabled) {
                        voldHookSkipped = true
                        logE(TAG) { "ProcessList - FUSE storage is not enabled, skip vold hook" }
                    } else {
                        val isolationEnabled = getBooleanField(
                            param.thisObject,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            processListClazz,
                        )

                        if (!isolationEnabled) {
                            setBooleanField(
                                param.thisObject,
                                VOLD_APPDATA_ISOLATION_ENABLED,
                                true,
                                processListClazz,
                            )

                            logI(TAG) { "ProcessList - Vold app data isolation is forced" }
                        }
                    }
                }
            }

            hookAfter(
                PROCESS_LIST_CLASS,
                "needsStorageDataIsolation",
            ) { param ->
                if (service.config.altVoldAppDataIsolation) {
                    val app = param.args.find { it?.javaClass?.simpleName == "ProcessRecord" }!!
                    val uid = runCatching {
                        getIntField(app, "uid")
                    }.getOrElse {
                        getIntField(app, "uid", processRecordIntClass)
                    }

                    val apps = getCallingApps(service, uid)

                    if (HMAService.instance?.config?.detailLog == true) {
                        val processName = runCatching {
                            getObjectField(app, "processName")
                        }.getOrElse {
                            getObjectField(app, "processName", processRecordIntClass)
                        }
                        val mountNode = runCatching {
                            getIntField(app, "mMountMode")
                        }.getOrDefault(0)
                        val isolated = runCatching {
                            getBooleanField(app, "isolated")
                        }.getOrElse {
                            getBooleanField(app, "isolated", processRecordIntClass)
                        }
                        val appZygote = runCatching {
                            getBooleanField(app, "appZygote")
                        }.getOrElse {
                            getBooleanField(app, "appZygote", processRecordIntClass)
                        }

                        logD(TAG) { "@needsStorageDataIsolation $uid and ${apps.contentToString()} - $processName value without override: ${param.result}, mount node: $mountNode, isolated: $isolated, appZygote: $appZygote" }
                    }

                    // Do not isolate this module for safety
                    if (apps.contains(BuildConfig.APP_PACKAGE_NAME)) {
                        param.result = false
                        return@hookAfter
                    }

                    if (apps.any { service.isAppDataIsolationExcluded(it) }) {
                        param.result = false
                        return@hookAfter
                    }

                    if (service.config.skipSystemAppDataIsolation) {
                        val isSystemApp = service.systemApps.any { apps.contains(it) }
                        logD(TAG) { "@needsStorageDataIsolation $uid and ${apps.contentToString()} - isSystemApp: $isSystemApp" }

                        if (isSystemApp) {
                            param.result = false
                            return@hookAfter
                        }
                    }
                }
            }

            hookBefore(
                STORAGE_MANAGER_SERVICE_CLASS,
                "onVolumeStateChangedLocked",
            ) { param ->
                if (service.config.altVoldAppDataIsolation && !voldHookSkipped) {
                    val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                    if (!fuseEnabled) {
                        logE(TAG) { "StorageManagerService - FUSE storage is not enabled, skip vold hook" }
                        voldHookSkipped = true
                        return@hookBefore
                    }

                    val isolationEnabled = getBooleanField(
                        param.thisObject,
                        VOLD_APPDATA_ISOLATION_ENABLED,
                    )

                    if (!isolationEnabled) {
                        setBooleanField(
                            param.thisObject,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            true,
                        )

                        logI(TAG) { "StorageManagerService - Vold app data isolation is forced" }
                    }
                }
            }

            hookBefore(
                STORAGE_MANAGER_SERVICE_CLASS,
                "remountAppStorageDirs",
            ) { param ->
                if (!voldHookSkipped && service.config.altVoldAppDataIsolation && service.config.skipSystemAppDataIsolation) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    val pidPkgMap = param.getArgument(1) as Map<*, *>
                    val keysToRemove = mutableSetOf<Any>()

                    for (entry in pidPkgMap.entrySet()) {
                        val pid = entry.key
                        val packageName = entry.value as String

                        if (packageName in service.systemApps || packageName == BuildConfig.APP_PACKAGE_NAME) {
                            logD(TAG) { "@remountAppStorageDirs SYSTEM $pid - $packageName is marked to remove" }
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
