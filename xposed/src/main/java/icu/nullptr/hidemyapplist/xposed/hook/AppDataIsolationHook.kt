package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Build
import android.os.SystemProperties
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import org.frknkrc44.hma_oss.common.BuildConfig

@RequiresApi(Build.VERSION_CODES.R)
class AppDataIsolationHook(private val service: HMAService): IFrameworkHook {

    companion object {
        private const val TAG = "AppDataIsolationHook"
        private const val APPDATA_ISOLATION_ENABLED = "mAppDataIsolationEnabled"
        private const val VOLD_APPDATA_ISOLATION_ENABLED = "mVoldAppDataIsolationEnabled"
        private const val FUSE_PROP = "persist.sys.fuse"
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()
    private var voldHookSkipped = false

    override fun load() {
        if (!(service.config.altAppDataIsolation || service.config.altVoldAppDataIsolation)) return
        logI(TAG, "Load hook")

        findMethodOrNull(
            "com.android.server.am.ProcessList"
        ) {
            name == "startProcess"
        }?.hookBefore { param ->
            if (service.config.altAppDataIsolation) {
                val isEnabled = XposedHelpers.getBooleanField(
                    param.thisObject,
                    APPDATA_ISOLATION_ENABLED
                )

                if (!isEnabled) {
                    XposedHelpers.setBooleanField(
                        param.thisObject,
                        APPDATA_ISOLATION_ENABLED,
                        true
                    )

                    logI(TAG, "ProcessList - App data isolation is forced")
                }
            }

            if (service.config.altVoldAppDataIsolation && !voldHookSkipped) {
                val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                if (!fuseEnabled) {
                    voldHookSkipped = true
                    logE(TAG, "ProcessList - FUSE storage is not enabled, skip vold hook")
                } else {
                    val isolationEnabled = XposedHelpers.getBooleanField(
                        param.thisObject,
                        VOLD_APPDATA_ISOLATION_ENABLED
                    )

                    if (!isolationEnabled) {
                        XposedHelpers.setBooleanField(
                            param.thisObject,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            true
                        )

                        logI(TAG, "ProcessList - Vold app data isolation is forced")
                    }
                }
            }
        }?.let {
            hooks += it
        }

        findMethodOrNull(
            "com.android.server.am.ProcessList"
        ) {
            name == "needsStorageDataIsolation"
        }?.hookAfter { param ->
            if (service.config.altVoldAppDataIsolation) {
                val app = param.args.find { it.javaClass.simpleName == "ProcessRecord" }
                val uid = XposedHelpers.getIntField(app, "uid")
                val processName = runCatching {
                    XposedHelpers.getObjectField(app, "processName")
                }.getOrDefault("<unknown>")
                val mountNode = runCatching {
                    XposedHelpers.getIntField(app, "mMountMode")
                }.getOrDefault(0)
                val isolated = runCatching {
                    XposedHelpers.getBooleanField(app, "isolated")
                }.getOrDefault(false)
                val appZygote = runCatching {
                    XposedHelpers.getBooleanField(app, "appZygote")
                }.getOrDefault(false)

                val apps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(uid)
                } ?: return@hookAfter

                logD(
                    TAG,
                    "@needsStorageDataIsolation $uid and ${apps.contentToString()} - $processName value without override: ${param.result}, mount node: $mountNode, isolated: $isolated, appZygote: $appZygote"
                )

                // Do not isolate this module for safety
                if (apps.contains(BuildConfig.APP_PACKAGE_NAME)) {
                    param.result = false
                    return@hookAfter
                }

                if (apps.any { service.isAppDataIsolationExcluded(it) }) {
                    param.result = false
                }

                if (service.config.skipSystemAppDataIsolation) {
                    val isSystemApp = service.systemApps.any { apps.contains(it) }
                    logD(
                        TAG,
                        "@needsStorageDataIsolation $uid and ${apps.contentToString()} - isSystemApp: $isSystemApp"
                    )

                    if (isSystemApp) {
                        param.result = false
                        return@hookAfter
                    }
                }
            }
        }?.let {
            hooks += it
        }

        findMethodOrNull(
            "com.android.server.StorageManagerService"
        ) {
            name == "onVolumeStateChangedLocked"
        }?.hookBefore { param ->
            runCatching {
                if (service.config.altVoldAppDataIsolation) {
                    val fuseEnabled = SystemProperties.getBoolean(FUSE_PROP, false)

                    if (!fuseEnabled) {
                        logE(TAG, "StorageManagerService - FUSE storage is not enabled, disable hooks")
                        unload()
                        return@hookBefore
                    }

                    val isolationEnabled = XposedHelpers.getBooleanField(
                        param.thisObject,
                        VOLD_APPDATA_ISOLATION_ENABLED
                    )

                    if (!isolationEnabled) {
                        XposedHelpers.setBooleanField(
                            param.thisObject,
                            VOLD_APPDATA_ISOLATION_ENABLED,
                            true
                        )

                        logI(TAG, "StorageManagerService - Vold app data isolation is forced")
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }?.let {
            hooks += it
        }

        findMethodOrNull(
            "com.android.server.StorageManagerService"
        ) {
            name == "remountAppStorageDirs"
        }?.hookBefore { param ->
            if (service.config.altVoldAppDataIsolation && service.config.skipSystemAppDataIsolation) {
                val pidPkgMap = param.args[0] as java.util.Map<*, *>
                val userId = param.args[1] as Int

                val keysToRemove = mutableSetOf<Any>()

                for (entry in pidPkgMap.entrySet()) {
                    val pid = entry.key
                    val packageName = entry.value as String

                    val apps = Utils.binderLocalScope {
                        val uid = Utils.getPackageUidCompat(service.pms, packageName, 0L, userId)
                        service.pms.getPackagesForUid(uid)
                    } ?: continue

                    for (app in apps) {
                        if (app in service.systemApps || app == BuildConfig.APP_PACKAGE_NAME) {
                            logD(
                                TAG,
                                "@remountAppStorageDirs SYSTEM $pid - $packageName is marked to remove"
                            )
                            keysToRemove += pid
                            break
                        }
                    }
                }

                keysToRemove.forEach { pidPkgMap.remove(it) }
            }
        }?.let {
            hooks += it
        }
    }

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
