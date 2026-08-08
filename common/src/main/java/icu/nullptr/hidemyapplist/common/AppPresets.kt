package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.util.Log
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import icu.nullptr.hidemyapplist.common.Utils.isSystemApp
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
    private val presetList = mutableMapOf<String, BasePreset>()

    private val manifestDataCache = mutableMapOf<String, String>()

    var loggerFunction: ((Int, () -> String) -> Unit)? = null

    companion object {
        val instance by lazy { AppPresets() }
    }

    fun readManifest(packageName: String, zipFile: ZipFile): String {
        // Run gc immediately if runs out of free memory
        if (Runtime.getRuntime().freeMemory() < 2048000) {
            manifestDataCache.clear()
            System.gc()
            loggerFunction?.invoke(Log.VERBOSE) { "@readManifest tried to clear the memory" }
        }

        var cache = manifestDataCache[packageName]
        if (cache == null) {
            loggerFunction?.invoke(Log.VERBOSE) { "@readManifest cache is null, reading manifest for $packageName" }

            val manifestFile = zipFile.getInputStream(
                zipFile.getEntry("AndroidManifest.xml")
            )
            val manifestBytes = manifestFile.use { it.readBytes() }
            cache = String(manifestBytes, Charsets.US_ASCII)
            manifestDataCache[packageName] = cache
        } else {
            loggerFunction?.invoke(Log.VERBOSE) { "@readManifest returning cache for $packageName" }
        }

        return cache
    }

    val presetNames by lazy { presetList.keys }
    fun getPresetByName(name: String) = presetList[name]

    fun importCache(cache: PresetCache) {
        cache.cache.forEach { (presetName, elements) ->
            getPresetByName(presetName)?.packageNames?.addAll(elements)
        }
        RiskyPackageUtils.instance.importCache(cache.riskyPackageCache)
    }

    fun exportCache() = PresetCache().apply {
        presetList.forEach { (k, v) -> cache[k] = v.packageNames.toMutableList() }
        riskyPackageCache.addAll(RiskyPackageUtils.instance.exportCache())
    }

    fun reloadPresets(appsList: List<ApplicationInfo>, fromScratch: Boolean) {
        if (!fromScratch) {
            val packageNames = appsList.mapTo(HashSet()) { it.packageName }
            RiskyPackageUtils.instance.removeAppsFromListIfNotExists(packageNames)

            presetList.values.forEach { preset ->
                preset.packageNames.removeIf { it !in packageNames }
            }

            // fromScratch = false is only called at boot process, we can return safely
            return
        }

        RiskyPackageUtils.instance.clearAppList()
        presetList.values.forEach { it.clearPackageList() }

        appsList.forEach { appInfo ->
            val packageName = appInfo.packageName

            if (packageName == "android") return@forEach

            runCatching {
                RiskyPackageUtils.instance.tryToAddIntoGMSConnectionList(appInfo) {
                    loggerFunction?.invoke(Log.DEBUG) { it }
                }
            }.onFailure { fail ->
                loggerFunction?.invoke(Log.ERROR) { fail.toString() }
            }

            presetList.values.forEach { preset ->
                if (preset.containsPackage(packageName)) return@forEach

                if (preset is AccessibilityAppsPreset && appInfo.isSystemApp()) return@forEach

                runCatching {
                    preset.addPackageInfoPreset(appInfo)
                }.onFailure { fail ->
                    loggerFunction?.invoke(Log.ERROR) { fail.toString() }
                }

                loggerFunction?.invoke(Log.DEBUG) { preset.toString() }
            }
        }

        manifestDataCache.clear()
    }

    fun containsPackage(presetName: String, packageName: String) =
        presetList[presetName]?.containsPackage(packageName) ?: false

    fun handlePackageAdded(
        pms: IPackageManager,
        packageName: String,
        onModifyCache: (preset: String) -> Unit,
    ) {
        if (presetList.any { it.value.containsPackage(packageName) }) {
            return
        }

        var appInfo: ApplicationInfo? = null
        var addedInAList = false

        presetList.forEach {
            if (!it.value.containsPackage(packageName)) {
                if (appInfo == null)
                    appInfo = pms.getPackageInfoCompat(packageName, 0, 0)?.applicationInfo

                if (appInfo != null) {
                    runCatching {
                        if (it.value.addPackageInfoPreset(appInfo!!)) {
                            onModifyCache(it.key)
                            loggerFunction?.invoke(Log.DEBUG) { "Package $packageName added into ${it.key}!" }
                            addedInAList = true
                        }
                    }.onFailure { fail ->
                        loggerFunction?.invoke(Log.ERROR) { fail.toString() }
                    }
                }
            }
        }

        if (appInfo == null)
            appInfo = pms.getPackageInfoCompat(packageName, 0, 0)?.applicationInfo

        if (appInfo != null)
            addedInAList = RiskyPackageUtils.instance.tryToAddIntoGMSConnectionList(appInfo) {
                loggerFunction?.invoke(Log.DEBUG) { it }
            } || addedInAList

        if (addedInAList)
            loggerFunction?.invoke(Log.DEBUG) { "Package add event handled for $packageName!" }

        manifestDataCache.clear()

        return
    }

    fun handlePackageRemoved(
        packageName: String,
        onModifyCache: (preset: String) -> Unit,
        ): Boolean {
        var itWasInAList = false

        presetList.forEach {
            if (it.value.removePackageFromPreset(packageName)) {
                onModifyCache(it.key)
                itWasInAList = true
            }
        }

        if (RiskyPackageUtils.instance.removeAppFromList(packageName))
            itWasInAList = true

        if (itWasInAList)
            loggerFunction?.invoke(Log.DEBUG) { "Package remove event handled for $packageName!" }

        return itWasInAList
    }

    init {
        presetList[CustomROMPreset.NAME] = CustomROMPreset()
        presetList[DetectorAppsPreset.NAME] = DetectorAppsPreset()
        presetList[RootAppsPreset.NAME] = RootAppsPreset(this)
        presetList[XposedModulesPreset.NAME] = XposedModulesPreset()
        presetList[SuspiciousAppsPreset.NAME] = SuspiciousAppsPreset()
        presetList[SDhizukuAppsPreset.NAME] = SDhizukuAppsPreset(this)
        presetList[AccessibilityAppsPreset.NAME] = AccessibilityAppsPreset(this)
    }
}
