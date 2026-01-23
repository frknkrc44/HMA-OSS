package icu.nullptr.hidemyapplist.xposed

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.isStatic
import com.github.kyuubiran.ezxhelper.utils.removeIf
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.PresetCacheHolder
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils.appHasGMSConnection
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.common.app_presets.DetectorAppsPreset
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.xposed.hook.AccessibilityHook
import icu.nullptr.hidemyapplist.xposed.hook.ActivityHook
import icu.nullptr.hidemyapplist.xposed.hook.AppDataIsolationHook
import icu.nullptr.hidemyapplist.xposed.hook.ContentProviderHook
import icu.nullptr.hidemyapplist.xposed.hook.IFrameworkHook
import icu.nullptr.hidemyapplist.xposed.hook.ImmHook
import icu.nullptr.hidemyapplist.xposed.hook.PlatformCompatHook
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget29
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget30
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget31
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget33
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget34
import icu.nullptr.hidemyapplist.xposed.hook.PmsPackageEventsHook
import icu.nullptr.hidemyapplist.xposed.hook.ZygoteHook
import org.frknkrc44.hma_oss.common.BuildConfig
import rikka.hidden.compat.ActivityManagerApis
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class HMAService(val pms: IPackageManager, val pmn: Any?) : IHMAService.Stub() {

    companion object {
        private const val TAG = "HMA-Service"
        var instance: HMAService? = null
    }

    @Volatile
    var logcatAvailable = false

    private lateinit var dataDir: String
    private lateinit var configFile: File
    private lateinit var presetCacheFile: File
    private lateinit var filterCountFile: File
    private lateinit var logFile: File
    private lateinit var oldLogFile: File

    private val configLock = Any()
    private val loggerLock = Any()
    val systemApps = mutableSetOf<String>()
    private val frameworkHooks = mutableSetOf<IFrameworkHook>()
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val uidHideCache = mutableListOf<Triple<Int, String, MutableList<String>>>()

    var config = JsonConfig().apply { detailLog = true }
        private set

    var presetCache = PresetCacheHolder()
        private set

    /*
    var filterCount = 0
        @JvmName("getFilterCountInternal") get
        set(value) {
            field = value

            if (value % 100 == 0) {
                GlobalScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
                    File("$dataDir/filter_count").writeText(value.toString())
                }
            }
        }
     */

    var filterHolder = FilterHolder()
        private set

    val totalFilterCount: Int get() = filterHolder.totalCount

    init {
        searchDataDir()
        instance = this
        loadFilterCount()
        loadConfig()
        loadPresetCache()
        installHooks()
        logI(TAG, "HMA service initialized")

        AppPresets.instance.loggerFunction = { level, msg ->
            logWithLevel(level, "AppPresets", msg)
        }

        thread {
            reloadPresets(false)
        }
    }

    private fun searchDataDir() {
        File("/data/system").list()?.forEach {
            if (it.startsWith("hide_my_applist")) {
                if (!this::dataDir.isInitialized) {
                    val newDir = File("/data/misc/$it")
                    File("/data/system/$it").renameTo(newDir)
                    dataDir = newDir.path
                } else {
                    File("/data/system/$it").deleteRecursively()
                }
            }
        }
        File("/data/misc").list()?.forEach {
            if (it.startsWith("hide_my_applist")) {
                if (!this::dataDir.isInitialized) {
                    dataDir = "/data/misc/$it"
                } else if (dataDir != "/data/misc/$it") {
                    File("/data/misc/$it").deleteRecursively()
                }
            }
        }
        if (!this::dataDir.isInitialized) {
            dataDir = "/data/misc/hide_my_applist_" + Utils.generateRandomString(16)
        }

        File("$dataDir/log").mkdirs()
        configFile = File("$dataDir/config.json")
        presetCacheFile = File("$dataDir/preset_cache.json")
        filterCountFile = File("$dataDir/filter_count.json")
        logFile = File("$dataDir/log/runtime.log")
        oldLogFile = File("$dataDir/log/old.log")
        logFile.renameTo(oldLogFile)
        logFile.createNewFile()

        logcatAvailable = true
        logI(TAG, "Data dir: $dataDir")
    }

    private fun loadConfig() {
        // remove the old filter count
        File("$dataDir/filter_count").also {
            runCatching {
                if (it.exists()) it.delete()
            }.onFailure { e ->
                logW(TAG, "Failed to load filter count, set to 0", e)
                it.writeText("0")
            }
        }
        if (!configFile.exists()) {
            logI(TAG, "Config file not found")
            return
        }
        val loading = runCatching {
            val json = configFile.readText()
            JsonConfig.parse(json)
        }.getOrElse {
            logE(TAG, "Failed to parse config.json", it)
            return
        }
        if (loading.configVersion != BuildConfig.CONFIG_VERSION) {
            logW(TAG, "Config version mismatch, need to reload")
            return
        }
        config = loading
        logI(TAG, "Config loaded")
    }

    private fun loadPresetCache() {
        if (!presetCacheFile.exists()) {
            logI(TAG, "Preset cache file not found")
            return
        }
        val loading = runCatching {
            val json = presetCacheFile.readText()
            PresetCacheHolder.parse(json)
        }.getOrElse {
            logE(TAG, "Failed to parse preset_cache.json", it)
            return
        }
        presetCache = loading
        logI(TAG, "Preset cache loaded")
    }

    private fun loadFilterCount() {
        if (!filterCountFile.exists()) {
            logI(TAG, "Filter count file not found")
            return
        }
        val loading = runCatching {
            val json = filterCountFile.readText()
            FilterHolder.parse(json)
        }.getOrElse {
            logE(TAG, "Failed to parse filter_count.json", it)
            return
        }
        filterHolder = loading
        logI(TAG, "Filter counts loaded")
    }

    private fun installHooks() {
        Utils.getInstalledApplicationsCompat(pms, 0, 0).mapNotNullTo(systemApps) {
            if (it.flags and ApplicationInfo.FLAG_SYSTEM != 0) it.packageName else null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            frameworkHooks.add(PmsHookTarget34(this))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            frameworkHooks.add(PmsHookTarget33(this))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            frameworkHooks.add(PmsHookTarget31(this))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(PmsHookTarget30(this))
        } else {
            frameworkHooks.add(PmsHookTarget29(this))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(PlatformCompatHook(this))
            frameworkHooks.add(AppDataIsolationHook(this))
        }

        frameworkHooks.add(ActivityHook(this))
        frameworkHooks.add(PmsPackageEventsHook(this))
        frameworkHooks.add(AccessibilityHook(this))
        frameworkHooks.add(ContentProviderHook(this))
        frameworkHooks.add(ImmHook(this))
        frameworkHooks.add(ZygoteHook(this))

        frameworkHooks.forEach(IFrameworkHook::load)
        logI(TAG, "Hooks installed")
    }

    fun increasePMFilterCount(callingUid: Int, amount: Int = 1) = increaseFilterCount(
        callingUid, amount, FilterHolder.FilterType.PACKAGE_MANAGER
    )

    fun increasePMFilterCount(caller: String?, amount: Int = 1) = increaseFilterCount(
        caller, amount, FilterHolder.FilterType.PACKAGE_MANAGER
    )

    fun increaseALFilterCount(caller: String?, amount: Int = 1) = increaseFilterCount(
        caller, amount, FilterHolder.FilterType.ACTIVITY_LAUNCH
    )

    fun increaseInstallerFilterCount(caller: String?, amount: Int = 1) = increaseFilterCount(
        caller, amount, FilterHolder.FilterType.INSTALLER
    )

    fun increaseSettingsFilterCount(caller: String?, amount: Int = 1) = increaseFilterCount(
        caller, amount, FilterHolder.FilterType.SETTINGS
    )

    fun increaseOthersFilterCount(caller: String?, amount: Int = 1) = increaseFilterCount(
        caller, amount, FilterHolder.FilterType.OTHERS
    )

    fun increaseFilterCount(uid: Int, amount: Int = 1, filterType: FilterHolder.FilterType) {
        if (amount < 1) return

        val caller = uidHideCache.firstOrNull { it.first == uid }?.second
        if (caller == null) return

        return increaseFilterCount(caller, amount, filterType)
    }

    fun increaseFilterCount(caller: String?, amount: Int = 1, filterType: FilterHolder.FilterType) {
        if (caller == null || amount < 1) return

        if (!filterHolder.filterCounts.containsKey(caller)) {
            filterHolder.filterCounts[caller] = FilterHolder.FilterCount()
        }

        val filterCount = filterHolder.filterCounts[caller]!!
        when (filterType) {
            FilterHolder.FilterType.PACKAGE_MANAGER -> filterCount.packageManagerCount += amount
            FilterHolder.FilterType.ACTIVITY_LAUNCH -> filterCount.activityLaunchCount += amount
            FilterHolder.FilterType.INSTALLER -> filterCount.installerCount += amount
            FilterHolder.FilterType.SETTINGS -> filterCount.settingsCount += amount
            FilterHolder.FilterType.OTHERS -> filterCount.othersCount += amount
        }

        executor.execute { writeFilterCount() }
    }

    fun isHookEnabled(packageName: String?) = config.scope.containsKey(packageName)

    fun isAppDataIsolationExcluded(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false

        return config.scope[packageName]?.excludeVoldIsolation ?: false
    }

    fun getSpoofedSetting(caller: String?, name: String?, database: String): ReplacementItem? {
        if (caller == null || name == null) return null

        val templates = getEnabledSettingsTemplates(caller)
        val replacement = config.settingsTemplates.firstNotNullOfOrNull { (key, value) ->
            if (key in templates) value.settingsList.firstOrNull { it.name == name } else null
        }
        if (replacement != null) return replacement

        val presets = getEnabledSettingsPresets(caller)
        if (presets.isNotEmpty()) {
            for (presetName in presets) {
                val preset = SettingsPresets.instance.getPresetByName(presetName)
                val replacement = preset?.getSpoofedValue(name)
                if (replacement?.database == database) return replacement
            }
        }

        return null
    }

    fun getEnabledSettingsTemplates(caller: String?): Set<String> {
        if (caller == null) return setOf()
        return config.scope[caller]?.applySettingTemplates ?: return setOf()
    }

    fun getEnabledSettingsPresets(caller: String?): Set<String> {
        if (caller == null) return setOf()
        return config.scope[caller]?.applySettingsPresets ?: return setOf()
    }

    fun isAppInGMSIgnoredPackages(caller: String, query: String) =
        (caller in Constants.gmsPackages) && appHasGMSConnection(query)

    fun shouldHideFromUid(uid: Int, query: String?): Boolean? {
        if (query == null) return null

        return uidHideCache.any { it.first == uid && it.third.contains(query) }
    }

    fun putShouldHideUidCache(uid: Int, caller: String, query: String) {
        val findList = uidHideCache.firstOrNull { it.first == uid }
        if (findList != null) {
            findList.third.add(query)
        } else {
            uidHideCache.add(Triple(uid, caller, mutableListOf(query)))
        }
    }

    fun shouldHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller == BuildConfig.APP_PACKAGE_NAME) return false
        if (caller in Constants.packagesShouldNotHide || query in Constants.packagesShouldNotHide) return false
        if (caller == query) return false
        val appConfig = config.scope[caller] ?: return false

        if (query in appConfig.extraAppList) return !appConfig.useWhitelist
        if (query in appConfig.extraOppositeAppList) return appConfig.useWhitelist

        for (tplName in appConfig.applyTemplates) {
            val tpl = config.templates[tplName]!!
            if (query in tpl.appList) {
                if (isAppInGMSIgnoredPackages(caller, query)) return false

                return !appConfig.useWhitelist
            }
        }

        for (presetName in appConfig.applyPresets) {
            val preset = AppPresets.instance.getPresetByName(presetName) ?: continue

            if (preset.containsPackage(query)) {
                // Do not hide detector apps from Play Store if they are connected to GMS
                val overriddenCaller = if (presetName == DetectorAppsPreset.NAME && caller == Constants.VENDING_PACKAGE_NAME) {
                    Constants.GMS_PACKAGE_NAME
                } else {
                    caller
                }

                return !isAppInGMSIgnoredPackages(overriddenCaller, query)
            }
        }

        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        return appConfig.useWhitelist
    }

    fun getRestrictedZygotePermissions(caller: String?) =
        config.scope[caller]?.restrictedZygotePermissions

    fun shouldHideActivityLaunch(caller: String?, query: String?): Boolean {
        val appConfig = config.scope[caller]
        if (appConfig != null && shouldHide(caller, query)) {
            return if (appConfig.invertActivityLaunchProtection) {
                config.disableActivityLaunchProtection
            } else {
                !config.disableActivityLaunchProtection
            }
        }

        return false
    }

    fun shouldHideInstallationSource(caller: String?, query: String?, callingHandle: UserHandle): Int {
        if (caller == null || query == null) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (caller == BuildConfig.APP_PACKAGE_NAME) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        val appConfig = config.scope[caller] ?: return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (!appConfig.hideInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        logD(TAG, "@shouldHideInstallationSource $caller: $query")
        if (caller == query && appConfig.excludeTargetInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED

        try {
            val uid = Utils.getPackageUidCompat(pms, query, 0L, callingHandle.hashCode())
            logD(TAG, "@shouldHideInstallationSource UID for $caller, ${callingHandle.hashCode()}: $query, $uid")
            if (uid < 0) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED // invalid package installation source request
        } catch (e: Throwable) {
            logD(TAG, "@shouldHideInstallationSource UID error for $caller, ${callingHandle.hashCode()}", e)
            return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        }

        return if (query in systemApps) {
            if (appConfig.hideSystemInstallationSource) {
                Constants.FAKE_INSTALLATION_SOURCE_SYSTEM
            } else {
                Constants.FAKE_INSTALLATION_SOURCE_DISABLED
            }
        } else {
            Constants.FAKE_INSTALLATION_SOURCE_USER
        }
    }

    override fun stopService(cleanEnv: Boolean) {
        logI(TAG, "Stop service")
        synchronized(loggerLock) {
            logcatAvailable = false
        }
        synchronized(configLock) {
            frameworkHooks.forEach(IFrameworkHook::unload)
            frameworkHooks.clear()
            if (cleanEnv) {
                logI(TAG, "Clean runtime environment")
                File(dataDir).deleteRecursively()
                return
            }
        }
        instance = null
    }

    fun addLog(parsedMsg: String) {
        synchronized(loggerLock) {
            if (!logcatAvailable) return
            if (logFile.length() / 1024 > config.maxLogSize) clearLogs()
            logFile.appendText(parsedMsg)
        }
    }

    override fun writeConfig(json: String) {
        synchronized(configLock) {
            runCatching {
                val newConfig = JsonConfig.parse(json)
                if (newConfig.configVersion != BuildConfig.CONFIG_VERSION) {
                    logW(TAG, "Sync config: version mismatch, need reboot")
                    return
                }
                config = newConfig
                configFile.writeText(json)
                frameworkHooks.forEach(IFrameworkHook::onConfigChanged)
                uidHideCache.clear()

                // remove filter counts for apps if they are not in config
                filterHolder.filterCounts.removeIf { key, _ -> !config.scope.containsKey(key) }
                writeFilterCount(true)
            }.onSuccess {
                logD(TAG, "Config synced")
            }.onFailure {
                return@synchronized
            }
        }
    }

    fun writePresetCache() {
        synchronized(configLock) {
            runCatching {
                presetCacheFile.writeText(presetCache.toString())
            }.onSuccess {
                logD(TAG, "Preset cache saved")
            }.onFailure {
                return@synchronized
            }
        }
    }

    fun writeFilterCount(force: Boolean = false) {
        if (!force && totalFilterCount % 100 != 0) {
            return
        }

        runCatching {
            filterCountFile.writeText(filterHolder.toString())
        }.onSuccess {
            logD(TAG, "Filter count synced")
        }.onFailure {
            return@onFailure
        }
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = totalFilterCount

    override fun getLogs() = synchronized(loggerLock) {
        logFile.readText()
    }

    override fun clearLogs() {
        synchronized(loggerLock) {
            oldLogFile.delete()
            logFile.renameTo(oldLogFile)
            logFile.createNewFile()
        }
    }

    override fun handlePackageEvent(eventType: String?, packageName: String?, extras: Bundle?) {
        if (eventType == null || packageName == null) return

        AppPresets.instance.apply {
            when (eventType) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    if (handlePackageAdded(pms, packageName, presetCache)) {
                        writePresetCache()
                    }
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // ignore package updates
                    if (extras?.getBoolean(Intent.EXTRA_REPLACING) == true) {
                        return
                    }

                    if (handlePackageRemoved(packageName, presetCache)) {
                        writePresetCache()
                    }
                }
            }
        }
    }

    override fun getPackagesForPreset(presetName: String) =
        AppPresets.instance.getPresetByName(presetName)?.packages?.toTypedArray()

    override fun readConfig() = config.toString()

    override fun forceStop(packageName: String?, userId: Int) {
        Utils.binderLocalScope {
            runCatching {
                ActivityManagerApis.forceStopPackage(packageName, userId)
            }.onFailure { error ->
                this.log(Log.ERROR, TAG, error.stackTraceToString())
            }
        }
    }

    override fun log(level: Int, tag: String, message: String) {
        logWithLevel(level, tag, message)
    }

    override fun getPackageNames(userId: Int) = Utils.binderLocalScope {
        Utils.getInstalledPackagesCompat(pms, 0L, userId).map { it.packageName }.toTypedArray()
    }

    override fun getPackageInfo(
        packageName: String,
        userId: Int
    ) = Utils.binderLocalScope {
        Utils.getPackageInfoCompat(pms, packageName, 0L, userId)
    }

    override fun listAllSettings(databaseName: String): Array<String> {
        val settingClass = when (databaseName) {
            Constants.SETTINGS_GLOBAL -> Settings.Global::class.java
            Constants.SETTINGS_SECURE -> Settings.Secure::class.java
            Constants.SETTINGS_SYSTEM -> Settings.System::class.java
            else -> throw IllegalArgumentException("Invalid database name $databaseName")
        }

        val readableVariables = settingClass.declaredFields.mapNotNull { field ->
            if (field.isStatic && field.type.simpleName == "String") field.get(null) as String else null
        }

        return readableVariables.sorted().toTypedArray()
    }

    override fun getLogFileLocation(): String = logFile.absolutePath

    fun reloadPresets(clearPresets: Boolean) {
        presetCache = AppPresets.instance.reloadPresets(pms, presetCache, clearPresets)
        writePresetCache()
        logI(TAG, "All presets are loaded")
    }

    override fun reloadPresetsFromScratch() {
        presetCache.presetPackageNames.clear()
        presetCache.gmsDependentApps.clear()

        reloadPresets(true)
    }

    override fun getDetailedFilterStats() = filterHolder.toString()
}
