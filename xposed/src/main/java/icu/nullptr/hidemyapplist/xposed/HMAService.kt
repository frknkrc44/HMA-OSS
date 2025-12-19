package icu.nullptr.hidemyapplist.xposed

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.isStatic
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.common.JsonConfig
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
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget28
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

class HMAService(val pms: IPackageManager, val pmn: Any?) : IHMAService.Stub() {

    companion object {
        private const val TAG = "HMA-Service"
        var instance: HMAService? = null
    }

    @Volatile
    var logcatAvailable = false

    private lateinit var dataDir: String
    private lateinit var configFile: File
    private lateinit var logFile: File
    private lateinit var oldLogFile: File

    private val configLock = Any()
    private val loggerLock = Any()
    val systemApps = mutableSetOf<String>()
    private val frameworkHooks = mutableSetOf<IFrameworkHook>()
    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    var config = JsonConfig().apply { detailLog = true }
        private set

    var filterCount = 0
        @JvmName("getFilterCountInternal") get
        set(value) {
            field = value
            if (field % 100 == 0) {
                synchronized(configLock) {
                    File("$dataDir/filter_count").writeText(field.toString())
                }
            }
        }

    init {
        searchDataDir()
        instance = this
        loadConfig()
        installHooks()
        AppPresets.instance.loggerFunction = { level, msg -> logWithLevel(level, TAG, msg) }
        AppPresets.instance.reloadPresets(pms)
        logI(TAG, "HMA service initialized")
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
        logFile = File("$dataDir/log/runtime.log")
        oldLogFile = File("$dataDir/log/old.log")
        logFile.renameTo(oldLogFile)
        logFile.createNewFile()

        logcatAvailable = true
        logI(TAG, "Data dir: $dataDir")
    }

    private fun loadConfig() {
        File("$dataDir/filter_count").also {
            runCatching {
                if (it.exists()) filterCount = it.readText().toInt()
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
            frameworkHooks.add(PmsHookTarget28(this))
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
        config.scope[caller]?.restrictedZygotePermissions ?: emptyList()

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

    fun shouldHideInstallationSource(caller: String?, query: String?, user: UserHandle): Int {
        if (caller == null || query == null) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (caller == BuildConfig.APP_PACKAGE_NAME) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        val appConfig = config.scope[caller] ?: return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (!appConfig.hideInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        logD(TAG, "@shouldHideInstallationSource $caller: $query")
        if (caller == query && appConfig.excludeTargetInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED

        try {
            val uid = Utils.getPackageUidCompat(pms, query, 0L, user.hashCode())
            logD(TAG, "@shouldHideInstallationSource UID for $caller, ${user.hashCode()}: $query, $uid")
            if (uid < 0) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED // invalid package installation source request
        } catch (e: Throwable) {
            logD(TAG, "@shouldHideInstallationSource UID error for $caller, ${user.hashCode()}", e)
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
            }.onSuccess {
                logD(TAG, "Config synced")
            }.onFailure {
                return@synchronized
            }
        }
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterCount

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

    override fun handlePackageEvent(eventType: String?, packageName: String?) {
        if (packageName == null) return

        when (eventType) {
            Intent.ACTION_PACKAGE_ADDED -> AppPresets.instance.handlePackageAdded(pms, packageName)
            Intent.ACTION_PACKAGE_REMOVED -> AppPresets.instance.handlePackageRemoved(packageName)
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
}
