package icu.nullptr.hidemyapplist.xposed

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import android.os.UserHandle
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.CommonUtils
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils.appHasGMSConnection
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.hook.AccessibilityHook
import icu.nullptr.hidemyapplist.xposed.hook.ActivityHook
import icu.nullptr.hidemyapplist.xposed.hook.AppDataIsolationHook
import icu.nullptr.hidemyapplist.xposed.hook.ContentProviderHook
import icu.nullptr.hidemyapplist.xposed.hook.IFrameworkHook
import icu.nullptr.hidemyapplist.xposed.hook.PlatformCompatHook
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget28
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget30
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget31
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget33
import icu.nullptr.hidemyapplist.xposed.hook.PmsHookTarget34
import icu.nullptr.hidemyapplist.xposed.hook.PmsPackageEventsHook
import org.frknkrc44.hma_oss.common.BuildConfig
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HMAService(val pms: IPackageManager) : IHMAService.Stub() {

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
        AppPresets.instance.loggerFunction = { logD(TAG, it) }
        AppPresets.instance.reloadPresetsIfEmpty(pms)
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

        frameworkHooks.forEach(IFrameworkHook::load)
        logI(TAG, "Hooks installed")
    }

    fun isHookEnabled(packageName: String) = config.scope.containsKey(packageName)

    fun getEnabledSettingsPresets(caller: String?): Set<String> {
        if (caller == null) return setOf()
        return config.scope[caller]?.applySettingsPresets ?: return setOf()
    }

    fun isAppInGMSIgnoredPackages(caller: String, query: String) =
        (caller in CommonUtils.gmsPackages) && appHasGMSConnection(query)

    fun shouldHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller in Constants.packagesShouldNotHide || query in Constants.packagesShouldNotHide) return false
        if (caller == query) return false
        val appConfig = config.scope[caller] ?: return false
        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        if (query in appConfig.extraAppList) return !appConfig.useWhitelist
        for (tplName in appConfig.applyTemplates) {
            val tpl = config.templates[tplName]!!
            if (query in tpl.appList) {
                if (isAppInGMSIgnoredPackages(caller, query)) return false

                return !appConfig.useWhitelist
            }
        }

        if (!appConfig.useWhitelist) {
            for (presetName in appConfig.applyPresets) {
                val preset = AppPresets.instance.getPresetByName(presetName) ?: continue
                if (preset.containsPackage(query))
                    return !isAppInGMSIgnoredPackages(caller, query)
            }
        }

        return appConfig.useWhitelist
    }

    fun shouldHideActivityLaunch(caller: String?, query: String?): Boolean {
        if (shouldHide(caller, query)) {
            val appConfig = config.scope[caller]
            if (appConfig != null) {
                return if (appConfig.invertActivityLaunchProtection) {
                    config.disableActivityLaunchProtection
                } else {
                    !config.disableActivityLaunchProtection
                }
            }
        }

        return false
    }

    fun shouldHideInstallationSource(caller: String?, query: String?, user: UserHandle): Int {
        if (caller == null || query == null) return 0
        val appConfig = config.scope[caller] ?: return 0
        if (!appConfig.hideInstallationSource) return 0
        logD(TAG, "@shouldHideInstallationSource $caller: $query")
        if (caller == query && appConfig.excludeTargetInstallationSource) return 0

        try {
            val uid = Utils.getPackageUidCompat(pms, query, 0L, user.hashCode())
            logD(TAG, "@shouldHideInstallationSource UID for $caller, ${user.hashCode()}: $query, $uid")
            if (uid < 0) return 0 // invalid package installation source request
        } catch (e: Throwable) {
            logD(TAG, "@shouldHideInstallationSource UID error for $caller, ${user.hashCode()}", e)
            return 0
        }

        return if (query in systemApps) {
            if (appConfig.hideSystemInstallationSource) { 2 } else { 0 }
        } else { 1 }
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
}
