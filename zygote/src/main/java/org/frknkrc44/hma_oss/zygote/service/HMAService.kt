package org.frknkrc44.hma_oss.zygote.service

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.CollectionUtils.removeIf
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.PARCEL_TYPE_CONFIG
import icu.nullptr.hidemyapplist.common.Constants.PARCEL_TYPE_LOG
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils.appHasGMSConnection
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import icu.nullptr.hidemyapplist.common.Utils.cleanRemnantsFromConfig
import icu.nullptr.hidemyapplist.common.Utils.generateRandomString
import icu.nullptr.hidemyapplist.common.Utils.getInstalledApplicationsCompat
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import icu.nullptr.hidemyapplist.common.Utils.getPackageUidCompat
import icu.nullptr.hidemyapplist.common.Utils.isSystemApp
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.hook.AccessibilityHook
import org.frknkrc44.hma_oss.zygote.hook.ActivityHook
import org.frknkrc44.hma_oss.zygote.hook.AppDataIsolationHook
import org.frknkrc44.hma_oss.zygote.hook.BroadcastHook
import org.frknkrc44.hma_oss.zygote.hook.ContentProviderHook
import org.frknkrc44.hma_oss.zygote.hook.IFrameworkHook
import org.frknkrc44.hma_oss.zygote.hook.ImmHook
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget29
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget30
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget31
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget33
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget34
import org.frknkrc44.hma_oss.zygote.hook.PmsPackageEventsHook
import org.frknkrc44.hma_oss.zygote.hook.ZygoteHook
import org.frknkrc44.hma_oss.zygote.util.BrowserUtils.getDefaultBrowser
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logW
import org.frknkrc44.hma_oss.zygote.util.Logcat.logWithLevel
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.findAndVerifyAppSignature
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.packageManager
import org.frknkrc44.hma_oss.zygote.util.WebViewUtils.getWebviewProvider
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethodWithTypes
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.UserManagerApis
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Modifier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HMAService(val pms: IPackageManager, val pmn: Any?, private val managerWorkMode: Int) : IHMAService.Stub() {

    companion object {
        private const val TAG = "HMA-Service"
        var service: HMAService? = null
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
    internal var appUid = 0
        private set

    var config = JsonConfig().apply { detailLog = true }
        private set

    var filterHolder = FilterHolder()
        private set

    init {
        searchDataDir()
        service = this
        loadFilterCount()
        loadConfig()

        if (managerWorkMode == Constants.MANAGER_WORK_MODE_OK) {
            installHooks()
        }

        logI(TAG) { "HMA service initialized in mode $managerWorkMode" }

        AppPresets.instance.loggerFunction = { level, msg ->
            logWithLevel(level, "AppPresets") { msg }
        }
        reloadPresetsFromScratch()

        appUid = findAndVerifyAppSignature(pms)
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
            dataDir = "/data/misc/hide_my_applist_" + generateRandomString(
                16,
                ('a'..'z').toList()
            )
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
        logI(TAG) { "Data dir: $dataDir" }
    }

    private fun loadConfig() {
        // remove the old filter count
        File("$dataDir/filter_count").also {
            runCatching {
                if (it.exists()) it.delete()
            }.onFailure { e ->
                logW(TAG, e) { "Failed to delete filter count, skip it" }
            }
        }

        // remove the preset cache
        presetCacheFile.also {
            runCatching {
                if (it.exists()) it.delete()
            }.onFailure { e ->
                logW(TAG, e) { "Failed to delete preset cache, skip it" }
            }
        }

        if (!configFile.exists()) {
            logI(TAG) { "Config file not found" }
            return
        }
        val loading = runCatching {
            val json = configFile.readText()
            JsonConfig.parse(json)
        }.getOrElse {
            logE(TAG, it) { "Failed to parse config.json" }
            return
        }
        if (loading.configVersion != BuildConfig.CONFIG_VERSION) {
            logW(TAG) { "Config version mismatch, need to reload" }
            return
        }
        cleanRemnantsFromConfig(loading)
        config = loading
        logI(TAG) { "Config loaded" }
    }

    private fun loadFilterCount() {
        if (!filterCountFile.exists()) {
            logI(TAG) { "Filter count file not found" }
            return
        }
        val loading = runCatching {
            val json = filterCountFile.readText()
            FilterHolder.parse(json)
        }.getOrElse {
            logE(TAG, it) { "Failed to parse filter_count.json" }
            return
        }
        filterHolder = loading
        logI(TAG) { "Filter counts loaded" }
    }

    private fun installHooks() {
        getInstalledApplicationsCompat(pms, PackageManager.MATCH_ALL.toLong(), 0)
            .mapNotNullTo(systemApps) { appInfo ->
                if (appInfo.isSystemApp()) appInfo.packageName else null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            frameworkHooks.add(PmsHookTarget34())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            frameworkHooks.add(PmsHookTarget33())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            frameworkHooks.add(PmsHookTarget31())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(PmsHookTarget30())
        } else {
            frameworkHooks.add(PmsHookTarget29())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(AppDataIsolationHook())
        }

        frameworkHooks.add(ActivityHook())
        frameworkHooks.add(BroadcastHook())
        frameworkHooks.add(PmsPackageEventsHook())
        frameworkHooks.add(AccessibilityHook())
        frameworkHooks.add(ContentProviderHook())
        frameworkHooks.add(ImmHook())
        frameworkHooks.add(ZygoteHook())

        frameworkHooks.forEach(IFrameworkHook::load)
        logI(TAG) { "Hooks installed" }
    }

    fun increasePMFilterCount(callingUid: Int?, amount: Int = 1) = increaseFilterCount(
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

    fun increaseFilterCount(uid: Int?, amount: Int = 1, filterType: FilterHolder.FilterType) {
        if (uid == null || amount < 1) return

        val caller = HMAServiceCache.instance.findCallerByUid(uid) ?: return

        return increaseFilterCount(caller, amount, filterType)
    }

    fun increaseFilterCount(caller: String?, amount: Int = 1, filterType: FilterHolder.FilterType) {
        if (caller == null || amount < 1) return

        synchronized(configLock) {
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
        }

        writeFilterCount()
    }

    fun isHookEnabled(packageName: String?) = config.scope.containsKey(packageName)

    fun isAnySettingsReplacementsEnabled(packageName: String?) = config.scope[packageName]?.let {
        it.applySettingsPresets.isNotEmpty() || it.applySettingTemplates.isNotEmpty()
    } ?: false

    fun isAppDataIsolationExcluded(packageName: String?) =
        config.scope[packageName]?.excludeVoldIsolation ?: false

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
        return config.scope[caller]?.applySettingTemplates ?: return setOf()
    }

    fun getEnabledSettingsPresets(caller: String?): Set<String> {
        return config.scope[caller]?.applySettingsPresets ?: return setOf()
    }

    fun isAppInGMSIgnoredPackages(caller: String, query: String) =
        (caller in Constants.gmsPackages) && appHasGMSConnection(query)

    fun shouldHide(caller: String?, query: String?, userId: Int): Boolean {
        if (caller == null || query == null) return false
        if (caller == BuildConfig.APP_PACKAGE_NAME) return false
        if (caller in Constants.packagesShouldNotHide || query in Constants.packagesShouldNotHide) return false
        if (caller == query) return false
        val appConfig = config.scope[caller] ?: return false

        if (config.webViewProtection) {
            // check for current webview
            val webviewProvider = getWebviewProvider()
            if (webviewProvider == caller || webviewProvider == query) return false

            // check for current browser
            val currentBrowser = getDefaultBrowser(userId)
            if (currentBrowser == caller || currentBrowser == query) return false
        }

        if (query in appConfig.extraAppList) return !appConfig.useWhitelist
        if (query in appConfig.extraOppositeAppList) return appConfig.useWhitelist

        for (tplName in appConfig.applyTemplates) {
            val tpl = config.templates[tplName] ?: continue
            if (query in tpl.appList) {
                if (isAppInGMSIgnoredPackages(caller, query)) return false

                return !appConfig.useWhitelist
            }
        }

        for (presetName in appConfig.applyPresets) {
            val preset = AppPresets.instance.getPresetByName(presetName) ?: continue

            if (preset.containsPackage(query)) {
                // Do not hide apps from Play Store if they are connected to GMS
                val overriddenCaller = if (caller == Constants.VENDING_PACKAGE_NAME) {
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

    fun shouldHideActivityLaunch(caller: String?, query: String?, userId: Int): Boolean {
        val appConfig = config.scope[caller]
        if (appConfig != null && shouldHide(caller, query, userId)) {
            return if (appConfig.invertActivityLaunchProtection) {
                config.disableActivityLaunchProtection
            } else {
                !config.disableActivityLaunchProtection
            }
        }

        return false
    }

    fun shouldHideInstallationSource(caller: String?, query: String?, callingUser: Int): Int {
        if (caller == null || query == null) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (caller == BuildConfig.APP_PACKAGE_NAME) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        val appConfig = config.scope[caller] ?: return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        if (!appConfig.hideInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED
        logD(TAG) { "@shouldHideInstallationSource $caller: $query" }
        if (caller == query && appConfig.excludeTargetInstallationSource) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED

        try {
            val uid = getPackageUidCompat(pms, query, 0L, callingUser)
            logD(TAG) { "@shouldHideInstallationSource UID for $caller, ${callingUser}: $query, $uid" }
            if (uid < 0) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED // invalid package installation source request
        } catch (e: Throwable) {
            logD(TAG, e) { "@shouldHideInstallationSource UID error for $caller, $callingUser" }
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
        if (!cleanEnv) return

        logI(TAG) { "Clean runtime environment" }
        File(dataDir).deleteRecursively()
    }

    fun ensureManagerWorkModeOK(silent: Boolean = false): Boolean {
        if (managerWorkMode == Constants.MANAGER_WORK_MODE_NO_HOOKS) {
            if (!silent) logW(TAG) { "Cannot write while in no hooks mode" }
            return false
        }

        return true
    }

    fun addLog(parsedMsg: String) {
        if (!ensureManagerWorkModeOK(true)) return

        synchronized(loggerLock) {
            if (!logcatAvailable) return
            if (logFile.length() / 1024 > config.maxLogSize) clearLogs()
            logFile.appendText(parsedMsg)
        }
    }

    override fun writeConfig(json: String) {
        if (!ensureManagerWorkModeOK()) return

        synchronized(configLock) {
            runCatching {
                val newConfig = JsonConfig.parse(json)
                cleanRemnantsFromConfig(newConfig)
                if (newConfig.configVersion != BuildConfig.CONFIG_VERSION) {
                    logW(TAG) { "Sync config: version mismatch, need reboot" }
                    return
                }
                config = newConfig
                configFile.writeText(json)
                HMAServiceCache.instance.clearUidCache()

                // remove filter counts for apps if they are not in config
                filterHolder.filterCounts.removeIf { key, _ -> !config.scope.containsKey(key) }
            }.onSuccess {
                logD(TAG) { "Config synced" }
            }.onFailure {
                return@synchronized
            }
        }

        writeFilterCount(true)
    }

    private fun writeFilterCount(force: Boolean = false) {
        if (!ensureManagerWorkModeOK()) return

        synchronized(configLock) {
            if (!force && filterHolder.totalCount % 100 != 0) {
                return
            }

            runCatching {
                filterCountFile.writeText(filterHolder.toString())
            }.onSuccess {
                logD(TAG) { "Filter count synced" }
            }.onFailure {
                return@onFailure
            }
        }
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterHolder.totalCount

    override fun getLogs() = synchronized(loggerLock) {
        logFile.readText()
    }

    override fun clearLogs() {
        if (!ensureManagerWorkModeOK()) return

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
                    if (packageName == BuildConfig.APP_PACKAGE_NAME && appUid < 0) {
                        appUid = findAndVerifyAppSignature(pms)
                    }

                    /**
                     * - Ignore when the default config was not available
                     * - Ignore for the manager app
                     * - Ignore for the package updates
                     * - Ignore when the target app had a config
                     */
                    val isDefConfigApplied = config.defaultConfig != null &&
                            packageName != BuildConfig.APP_PACKAGE_NAME &&
                            extras?.getBoolean(Intent.EXTRA_REPLACING) != true &&
                            config.scope.putIfAbsent(packageName, config.defaultConfig!!) == null

                    if (isDefConfigApplied) {
                        writeConfig(config.toString())
                    }

                    // Handle app presets
                    handlePackageAdded(pms, packageName)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // ignore package updates
                    if (extras?.getBoolean(Intent.EXTRA_REPLACING) == true) {
                        return
                    }

                    if (packageName == BuildConfig.APP_PACKAGE_NAME && appUid >= 0) {
                        logI(TAG) { "The manager app is uninstalled, looking for alternatives" }

                        appUid = findAndVerifyAppSignature(pms)
                    }

                    handlePackageRemoved(packageName)
                }
            }
        }
    }

    override fun getPackagesForPreset(presetName: String) =
        AppPresets.instance.getPresetByName(presetName)?.packages?.toTypedArray()

    override fun readConfig() = config.toString()

    override fun forceStop(packageName: String?, userId: Int) {
        binderLocalScope {
            runCatching {
                ActivityManagerApis.forceStopPackage(packageName, userId)
            }.onFailure { error ->
                this.log(Log.ERROR, TAG, error.stackTraceToString())
            }
        }
    }

    override fun log(level: Int, tag: String, message: String) {
        logWithLevel(level, tag) { message }
    }

    override fun getPackageNames(userId: Int) = binderLocalScope {
        getInstalledApplicationsCompat(pms, 0L, userId).map { it.packageName }.toTypedArray()
    }

    override fun getPackageInfo(
        packageName: String,
        userId: Int
    ) = binderLocalScope {
        getPackageInfoCompat(pms, packageName, 0L, userId)
    }

    override fun listAllSettings(databaseName: String): Array<String> {
        val settingClass = when (databaseName) {
            Constants.SETTINGS_GLOBAL -> Settings.Global::class.java
            Constants.SETTINGS_SECURE -> Settings.Secure::class.java
            Constants.SETTINGS_SYSTEM -> Settings.System::class.java
            else -> throw IllegalArgumentException("Invalid database name $databaseName")
        }

        val readableVariables = settingClass.declaredFields.mapNotNull { field ->
            if (Modifier.isStatic(field.modifiers) && field.type.simpleName == "String") field.get(null) as String else null
        }

        return readableVariables.sorted().toTypedArray()
    }

    override fun getLogFileLocation(): String = logFile.absolutePath

    override fun reloadPresetsFromScratch() {
        val apps = mutableListOf<ApplicationInfo>().apply {
            binderLocalScope {
                UserManagerApis.getUserIdsNoThrow().forEach { id ->
                    addAll(getInstalledApplicationsCompat(pms, 0L, id))
                }
            }
        }

        AppPresets.instance.reloadPresets(apps)
        logI(TAG) { "All presets are loaded" }
    }

    override fun getDetailedFilterStats() = filterHolder.toString()

    override fun clearFilterStats() {
        synchronized(configLock) {
            filterHolder.filterCounts.clear()
        }

        writeFilterCount(true)
    }

    override fun getServiceVersionName() = BuildConfig.APP_VERSION_NAME

    override fun getLoadedHooks(): Array<String> {
        val hookList = mutableListOf<String>()

        for ((className, hookElements) in BulkHooker.instance.hooks) {
            for (element in hookElements) {
                hookList.add(
                    JsonConfig.HookItem(
                        className,
                        element.methodName,
                        element.paramCount,
                    ).toString()
                )
            }
        }

        return hookList.toTypedArray()
    }

    override fun readFD(type: Int): ParcelFileDescriptor {
        return when (type) {
            PARCEL_TYPE_LOG -> {
                ParcelFileDescriptor.open(logFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            PARCEL_TYPE_CONFIG -> {
                ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            else -> throw RemoteException("Invalid type for read: $type")
        }
    }

    override fun writeFD(type: Int, fd: ParcelFileDescriptor) {
        val receiveStream = FileInputStream(fd.fileDescriptor)

        when (type) {
            PARCEL_TYPE_CONFIG -> {
                writeConfig(receiveStream.readBytes().decodeToString())
            }
            else -> throw RemoteException("Invalid type for write: $type")
        }

        receiveStream.close()
        fd.close()
    }

    override fun getManagerWorkMode() = managerWorkMode

    override fun startMainActivityAsUser(packageName: String, userId: Int) = binderLocalScope {
        val pkgInfo = getPackageInfoCompat(pms, packageName, 0, userId)
                ?: throw RemoteException("Cannot find package info for $packageName")

        if (pkgInfo.applicationInfo?.enabled == true) {
            val intentToLaunch = getLaunchIntentForPackageAsUser(packageName, userId)
            if (intentToLaunch != null) {
                ActivityManagerApis.startActivity(intentToLaunch, null, userId)
            } else {
                throw RemoteException("No main activity found to launch this app")
            }
        } else {
            throw RemoteException("Package is disabled")
        }
    }

    // This part is a copy of Android code
    fun getLaunchIntentForPackageAsUser(packageName: String, userId: Int): Intent? {
        // I am lazy to call IPackageManager
        @Suppress("UNCHECKED_CAST")
        fun queryIntentActivitiesAsUser(intent: Intent, userId: Int) = callMethodWithTypes(
            packageManager,
            "queryIntentActivitiesAsUser",
            arrayOf(
                Intent::class.java,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
            ),
            arrayOf(intent, /* flags */ 0, userId)
        ) as List<ResolveInfo>?

        val intentToResolve = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_INFO)
            setPackage(packageName)
        }

        var resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId)
        if (resolveInfos.isNullOrEmpty()) {
            intentToResolve.apply {
                removeCategory(Intent.CATEGORY_INFO)
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }

            resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId)
        }

        return if (resolveInfos.isNullOrEmpty()) {
            null
        } else {
            Intent(intentToResolve).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                resolveInfos.first().activityInfo.let {
                    setClassName(it.packageName, it.name)
                }
            }
        }
    }
}
