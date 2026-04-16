package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.util.Log
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils.ignoredForRiskyPackagesList
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils.tryToAddIntoGMSConnectionList
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import icu.nullptr.hidemyapplist.common.app_presets.AccessibilityAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.BasePreset
import icu.nullptr.hidemyapplist.common.app_presets.CustomROMPreset
import icu.nullptr.hidemyapplist.common.app_presets.DetectorAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.RootAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.SDhizukuAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.SuspiciousAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.XposedModulesPreset
import java.util.zip.ZipFile

class AppPresets private constructor() {
    private val presetList = mutableListOf<BasePreset>()

    private val manifestDataCache = mutableMapOf<String, String>()

    var loggerFunction: ((Int, String) -> Unit)? = null

    companion object {
        val instance by lazy { AppPresets() }
    }

    fun readManifest(packageName: String, zipFile: ZipFile): String {
        // Run gc immediately if runs out of free memory
        if (Runtime.getRuntime().freeMemory() < 2048000) {
            manifestDataCache.clear()
            System.gc()
            loggerFunction?.invoke(Log.VERBOSE, "@readManifest tried to clear the memory")
        }

        var cache = manifestDataCache[packageName]
        if (cache == null) {
            loggerFunction?.invoke(Log.VERBOSE, "@readManifest cache is null, reading manifest for $packageName")

            val manifestFile = zipFile.getInputStream(
                zipFile.getEntry("AndroidManifest.xml")
            )
            val manifestBytes = manifestFile.use { it.readBytes() }
            cache = String(manifestBytes, Charsets.US_ASCII)
            manifestDataCache[packageName] = cache
        } else {
            loggerFunction?.invoke(Log.VERBOSE, "@readManifest returning cache for $packageName")
        }

        return cache
    }

    val presetNames by lazy { presetList.map { it.name }.toTypedArray() }
    fun getPresetByName(name: String) = presetList.firstOrNull { it.name == name }

    fun reloadPresets(appsList: List<ApplicationInfo>) {
        ignoredForRiskyPackagesList.clear()
        presetList.forEach { it.clearPackageList() }

        for (appInfo in appsList) {
            when (appInfo.packageName) {
                "android" -> continue
            }

            runCatching {
                tryToAddIntoGMSConnectionList(appInfo, appInfo.packageName) {
                    loggerFunction?.invoke(Log.DEBUG, it)
                }
            }.onFailure { fail ->
                loggerFunction?.invoke(Log.ERROR, fail.toString())
            }

            presetList.forEach {
                runCatching {
                    it.addPackageInfoPreset(appInfo)
                }.onFailure { fail ->
                    loggerFunction?.invoke(Log.ERROR, fail.toString())
                }
            }
        }

        presetList.forEach { loggerFunction?.invoke(Log.DEBUG, it.toString()) }

        manifestDataCache.clear()
    }

    fun handlePackageAdded(pms: IPackageManager, packageName: String) {
        if (presetList.any { it.containsPackage(packageName) }) {
            return
        }

        var appInfo: ApplicationInfo? = null
        var addedInAList = false

        presetList.forEach {
            if (!it.containsPackage(packageName)) {
                if (appInfo == null)
                    appInfo = getPackageInfoCompat(pms, packageName, 0, 0)?.applicationInfo

                if (appInfo != null) {
                    runCatching {
                        if (it.addPackageInfoPreset(appInfo!!)) {
                            loggerFunction?.invoke(Log.DEBUG, "Package $packageName added into ${it.name}!")
                            addedInAList = true
                        }
                    }.onFailure { fail ->
                        loggerFunction?.invoke(Log.ERROR, fail.toString())
                    }
                }
            }
        }

        if (appInfo == null)
            appInfo = getPackageInfoCompat(pms, packageName, 0, 0)?.applicationInfo

        if (appInfo != null)
            addedInAList = tryToAddIntoGMSConnectionList(appInfo, packageName) {
                loggerFunction?.invoke(Log.DEBUG, it)
            } || addedInAList

        if (addedInAList)
            loggerFunction?.invoke(Log.DEBUG, "Package add event handled for $packageName!")

        manifestDataCache.clear()
    }

    fun handlePackageRemoved(packageName: String) {
        var itWasInAList = false

        presetList.forEach {
            if (it.removePackageFromPreset(packageName)) {
                itWasInAList = true
            }
        }

        if (ignoredForRiskyPackagesList.remove(packageName))
            itWasInAList = true

        if (itWasInAList)
            loggerFunction?.invoke(Log.DEBUG, "Package remove event handled for $packageName!")
    }

    init {
        presetList.add(CustomROMPreset())
        presetList.add(DetectorAppsPreset())
        presetList.add(RootAppsPreset(this))
        presetList.add(XposedModulesPreset())
        presetList.add(SuspiciousAppsPreset())
        presetList.add(SDhizukuAppsPreset(this))
        presetList.add(AccessibilityAppsPreset(this))
    }
}
