package org.frknkrc44.hma_oss.zygote.service;

import static org.frknkrc44.hma_oss.zygote.util.BrowserUtils.getDefaultBrowser;
import static org.frknkrc44.hma_oss.zygote.util.BrowserUtils.getWebviewProvider;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.appendText;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.deleteRecursively;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.ensureFileIsRW;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.readStream;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.readText;
import static org.frknkrc44.hma_oss.zygote.util.FileUtils.writeText;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logW;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logWithLevel;
import static org.frknkrc44.hma_oss.zygote.util.PackageManagerUtils.findApp;
import static org.frknkrc44.hma_oss.zygote.util.PackageManagerUtils.getLaunchIntentForPackageAsUser;
import static org.frknkrc44.hma_oss.zygote.util.PackageManagerUtils.isConflictingModuleInstalled;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.binderLocalScope;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.binderLocalScopeNoReturn;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.findAndVerifyAppSignature;

import static icu.nullptr.hidemyapplist.common.Utils.cleanRemnantsFromConfig;
import static icu.nullptr.hidemyapplist.common.Utils.conflictedModules;
import static icu.nullptr.hidemyapplist.common.Utils.generateRandomHex;
import static icu.nullptr.hidemyapplist.common.Utils.getInstalledApplicationsCompat;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.hook.AccessibilityHook;
import org.frknkrc44.hma_oss.zygote.hook.ActivityHook;
import org.frknkrc44.hma_oss.zygote.hook.AppDataIsolationHook;
import org.frknkrc44.hma_oss.zygote.hook.BroadcastHook;
import org.frknkrc44.hma_oss.zygote.hook.ContentProviderHook;
import org.frknkrc44.hma_oss.zygote.hook.IFrameworkHook;
import org.frknkrc44.hma_oss.zygote.hook.ImmHook;
import org.frknkrc44.hma_oss.zygote.hook.InstallerHookTarget29;
import org.frknkrc44.hma_oss.zygote.hook.InstallerHookTarget30;
import org.frknkrc44.hma_oss.zygote.hook.InstallerHookTarget33;
import org.frknkrc44.hma_oss.zygote.hook.InstallerHookTarget34;
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget29;
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget30;
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget31;
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget33;
import org.frknkrc44.hma_oss.zygote.hook.PmsHookTarget34;
import org.frknkrc44.hma_oss.zygote.hook.PmsPackageEventsHook;
import org.frknkrc44.hma_oss.zygote.hook.ZygoteHook;
import org.frknkrc44.hma_oss.zygote.util.ActivityManagerUtils;
import org.frknkrc44.hma_oss.zygote.util.UserManagerUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import icu.nullptr.hidemyapplist.common.AppPresets;
import icu.nullptr.hidemyapplist.common.CollectionUtils;
import icu.nullptr.hidemyapplist.common.Constants;
import icu.nullptr.hidemyapplist.common.FilterHolder;
import icu.nullptr.hidemyapplist.common.IHMAService;
import icu.nullptr.hidemyapplist.common.JsonConfig;
import icu.nullptr.hidemyapplist.common.OSUtils;
import icu.nullptr.hidemyapplist.common.PresetCache;
import icu.nullptr.hidemyapplist.common.PropertyUtils;
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils;
import icu.nullptr.hidemyapplist.common.SettingsPresets;
import icu.nullptr.hidemyapplist.common.Utils;
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem;

@SuppressWarnings("RedundantThrows")
public class HMAService extends IHMAService.Stub {
    private static final String TAG = "HMA-Java-Service";
    private static final String DATA_DIR_PREFIX = "hide_my_applist";

    public final IPackageManager pms;
    public final Object pmn;

    private volatile boolean logcatAvailable = false;

    public final BulkHooker hooker = new BulkHooker();
    public final HMAServiceDataHolder dataHolder = new HMAServiceDataHolder();

    private int managerWorkMode = Constants.MANAGER_WORK_MODE_UNKNOWN;

    private String dataDir = null;
    private File configFile;
    private File presetCacheFileOld;
    private File presetCacheFileNew;
    private File filterCountFile;
    private File logFile;
    private File oldLogFile;
    private File moduleStatusFile;

    private final Object configLock = new Object();
    private final Object loggerLock = new Object();
    public final HashSet<String> systemApps = new HashSet<>();
    private final HashSet<IFrameworkHook> frameworkHooks = new HashSet<>();
    int appUid = -1;

    public JsonConfig config = new JsonConfig();

    HMAService(IPackageManager pms, Object pmn) {
        this.pms = pms;
        this.pmn = pmn;

        config.setDetailLog(true);

        try {
            if (isConflictingModuleInstalled(pms)) {
                logE(TAG, null, () -> "Conflicting module detected, skipping hook");
                managerWorkMode = Constants.MANAGER_WORK_MODE_NO_HOOKS;
            }
        } catch (Throwable ignored) {}

        searchDataDir();
        saveModuleStatus();
        UserService.setService(this);
        loadFilterCount();
        loadConfig();

        appUid = findAndVerifyAppSignature(pms);

        if (managerWorkMode != Constants.MANAGER_WORK_MODE_NO_HOOKS) {
            installHooks();

            if (hooker.hooksWasCrashed) {
                managerWorkMode = Constants.MANAGER_WORK_MODE_CRASHED;
            } else {
                AppPresets.Companion.getInstance().setLoggerFunction((level, msg) -> {
                    logWithLevel(level, "AppPresets", null, msg::invoke);

                    // noinspection all
                    return null;
                });

                loadPresetCache();

                managerWorkMode = Constants.MANAGER_WORK_MODE_OK;
            }

            saveModuleStatus();
        }

        logI(TAG, null, () -> "HMA service initialized in mode " + managerWorkMode);
    }

    private void searchDataDir() {
        final var dataSystem = new File("/data/system").list();
        if (dataSystem != null) {
            for (var file : dataSystem) {
                if (file.startsWith(DATA_DIR_PREFIX)) {
                    if (dataDir == null) {
                        final var newDir = new File("/data/misc", file);
                        // noinspection all
                        new File("/data/system", file).renameTo(newDir);
                        dataDir = newDir.getPath();
                    } else {
                        deleteRecursively(new File("/data/system", file));
                    }
                }
            }
        }

        final var dataMisc = new File("/data/misc").list();
        if (dataMisc != null) {
            for (var file : dataMisc) {
                if (file.startsWith(DATA_DIR_PREFIX)) {
                    if (dataDir == null) {
                        dataDir = "/data/misc/" + file;
                    } else {
                        deleteRecursively(new File("/data/misc", file));
                    }
                }
            }
        }

        if (dataDir == null) {
            dataDir = "/data/misc/" + DATA_DIR_PREFIX + "_" + generateRandomHex(16);
        }

        final var logDir = new File(dataDir, "log");
        // noinspection all
        logDir.mkdirs();

        configFile = new File(dataDir, "config.json");
        presetCacheFileOld = new File(dataDir, "preset_cache.json");
        presetCacheFileNew = new File(dataDir, "preset_cache_v2.json");
        filterCountFile = new File(dataDir, "filter_count.json");
        logFile = new File(logDir, "runtime.log");
        oldLogFile = new File(logDir, "old.log");
        moduleStatusFile = new File(dataDir, "status.json");

        try {
            clearLogs();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        logcatAvailable = true;

        logI(TAG, null, () -> "Data dir: " + dataDir);

        try {
            Files.copy(
                    new File("/proc/self/maps").toPath(),
                    new File(dataDir, "maps_module_thread.txt").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while copying the map file");
        }
    }

    private void saveModuleStatus() {
        try {
            ensureFileIsRW(moduleStatusFile, true);

            final var json = new JSONObject();
            json.put("workMode", managerWorkMode);
            json.put("managerUid", appUid);

            writeText(moduleStatusFile, json.toString());
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while writing the status JSON");
        }
    }

    private void loadConfig() {
        final var filterCountFile = new File(dataDir, "filter_count");
        if (filterCountFile.exists()) {
            // noinspection all
            filterCountFile.delete();
        }

        try {
            ensureFileIsRW(configFile, true);

            if (!configFile.exists()) {
                logI(TAG, null, () -> "Config file not found, loading defaults" );
                return;
            }

            final var raw = readText(configFile);
            config = JsonConfig.Companion.parse(raw);
            cleanRemnantsFromConfig(config);

            logI(TAG, null, () -> "Config loaded");
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while reading config");
        }
    }

    private void loadFilterCount() {
        try {
            ensureFileIsRW(filterCountFile, true);

            if (!filterCountFile.exists()) {
                logI(TAG, null, () -> "Filter count file not found" );
                return;
            }

            final var raw = readText(filterCountFile);
            dataHolder.setFilterHolder(FilterHolder.Companion.parse(raw));

            logI(TAG, null, () -> "Filter counts loaded");
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while reading filter count");
        }
    }

    private void loadPresetCache() {
        try {
            ensureFileIsRW(presetCacheFileOld, true);

            if (presetCacheFileOld.exists()) {
                // noinspection all
                presetCacheFileOld.delete();
            }
        } catch (Throwable e) {
            logW(TAG, e, () -> "Failed to delete preset cache, skip it");
        }

        var isFileAvailable = false;
        try {
            ensureFileIsRW(presetCacheFileNew, true);

            isFileAvailable = presetCacheFileNew.exists();
            if (isFileAvailable) {
                final var raw = readText(presetCacheFileNew);
                AppPresets.Companion.getInstance().importCache(PresetCache.Companion.parse(raw));
            }
        } catch (Throwable e) {
            isFileAvailable = false;
        }

        reloadPresets(!isFileAvailable);
    }

    private void installHooks() {
        try {
            for (var packageName : pms.getAllPackages()) {
                final var packageInfo = Utils.getPackageInfoCompat(pms, packageName, 0, 0);
                if (packageInfo != null &&
                    packageInfo.applicationInfo != null &&
                    Utils.isSystemApp(packageInfo.applicationInfo)) {
                    systemApps.add(packageName);
                }
            }
        } catch (Throwable e) {
            logE(TAG, e, () -> "Cannot load all packages list");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            frameworkHooks.add(new PmsHookTarget34());
            frameworkHooks.add(new InstallerHookTarget34());
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            frameworkHooks.add(new PmsHookTarget33());
            frameworkHooks.add(new InstallerHookTarget33());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            frameworkHooks.add(new PmsHookTarget31());
            frameworkHooks.add(new InstallerHookTarget30());
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            frameworkHooks.add(new PmsHookTarget30());
            frameworkHooks.add(new InstallerHookTarget30());
        } else {
            frameworkHooks.add(new PmsHookTarget29());
            frameworkHooks.add(new InstallerHookTarget29());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(new AppDataIsolationHook());
        }

        frameworkHooks.add(new ActivityHook());
        frameworkHooks.add(new BroadcastHook());
        frameworkHooks.add(new PmsPackageEventsHook());
        frameworkHooks.add(new AccessibilityHook());
        frameworkHooks.add(new ContentProviderHook());
        frameworkHooks.add(new ImmHook());
        frameworkHooks.add(new ZygoteHook());

        frameworkHooks.forEach(IFrameworkHook::load);
        logI(TAG, null, () -> "Hooks installed");
    }

    public void increasePMFilterCount(int callingUid) {
        increasePMFilterCount(callingUid, 1);
    }

    public void increasePMFilterCount(int callingUid, int amount) {
        dataHolder.increaseFilterCount(callingUid, amount, FilterHolder.FilterType.PACKAGE_MANAGER, this::writeFilterCount);
    }

    public void increasePMFilterCount(String caller) {
        increasePMFilterCount(caller, 1);
    }

    public void increasePMFilterCount(String caller, int amount) {
        dataHolder.increaseFilterCount(caller, amount, FilterHolder.FilterType.PACKAGE_MANAGER, this::writeFilterCount);
    }

    public void increaseALFilterCount(String caller) {
        increaseALFilterCount(caller, 1);
    }

    public void increaseALFilterCount(String caller, int amount) {
        dataHolder.increaseFilterCount(caller, amount, FilterHolder.FilterType.ACTIVITY_LAUNCH, this::writeFilterCount);
    }

    public void increaseInstallerFilterCount(String caller) {
        increaseInstallerFilterCount(caller, 1);
    }

    public void increaseInstallerFilterCount(String caller, int amount) {
        dataHolder.increaseFilterCount(caller, amount, FilterHolder.FilterType.INSTALLER, this::writeFilterCount);
    }

    public void increaseSettingsFilterCount(String caller) {
        increaseSettingsFilterCount(caller, 1);
    }

    public void increaseSettingsFilterCount(String caller, int amount) {
        dataHolder.increaseFilterCount(caller, amount, FilterHolder.FilterType.SETTINGS, this::writeFilterCount);
    }

    public void increaseOthersFilterCount(String caller) {
        increaseOthersFilterCount(caller, 1);
    }

    public void increaseOthersFilterCount(String caller, int amount) {
        dataHolder.increaseFilterCount(caller, amount, FilterHolder.FilterType.OTHERS, this::writeFilterCount);
    }

    public boolean isHookEnabled(@Nullable String packageName) {
        return config.getScope().containsKey(packageName);
    }

    public boolean isAnySettingsReplacementsEnabled(@Nullable String packageName) {
        final var scope = config.getScope().get(packageName);
        return scope == null ||
                scope.getApplySettingsPresets().isEmpty() ||
                scope.getApplySettingTemplates().isEmpty();
    }

    public boolean isAppDataIsolationExcluded(String packageName) {
        if (OSUtils.isSamsung()) return false;
        if (PropertyUtils.isVoldAppDataIsolationEnabled()) return false;

        final var scope = config.getScope().get(packageName);
        return scope != null && scope.getExcludeVoldIsolation();
    }

    @Nullable
    public ReplacementItem getSpoofedSetting(@Nullable String caller, @Nullable String name, @NonNull String database) {
        if (caller == null || name == null) return null;

        final var appConfig = config.getScope().get(caller);
        if (appConfig == null) return null;

        final var templates = appConfig.getApplySettingTemplates();
        if (!templates.isEmpty()) {
            for (var entry : config.getSettingsTemplates().entrySet()) {
                if (templates.contains(entry.getKey())) {
                    for (var set : entry.getValue().getSettingsList()) {
                        if (name.equals(set.getName()) && database.equals(set.getDatabase())) {
                            return set;
                        }
                    }
                }
            }
        }

        final var presets = appConfig.getApplySettingsPresets();
        if (!presets.isEmpty()) {
            for (var presetName : presets) {
                final var preset = SettingsPresets.Companion.getInstance().getPresetByName(presetName);
                if (preset != null) {
                    final var set = preset.getSpoofedValue(name, database);
                    if (set != null) return set;
                }
            }
        }

        return null;
    }

    @Nullable
    public Set<String> getEnabledSettingsPresets(String caller) {
        final var scope = config.getScope().get(caller);
        return scope != null
                ? scope.getApplySettingsPresets()
                : null;
    }

    public boolean isAppInGMSIgnoredPackages(String caller, String query) {
        return Constants.getGmsPackages().contains(caller) &&
                RiskyPackageUtils.Companion.getInstance().appHasGMSConnection(query, false);
    }

    public boolean shouldHide(@Nullable String caller, @Nullable String query, int userId) {
        if (caller == null || query == null) return false;
        if (caller.equals(query)) return false;
        if (BuildConfig.APP_PACKAGE_NAME.equals(caller)) return false;

        final var shouldNotHide = Constants.getPackagesShouldNotHide();
        if (shouldNotHide.contains(caller) || shouldNotHide.contains(query)) return false;

        final var appConfig = config.getScope().get(caller);
        if (appConfig == null) return false;

        if (config.getWebViewProtection()) {
            // check for current webview
            final var webViewProvider = getWebviewProvider();
            if (caller.equals(webViewProvider) || query.equals(webViewProvider)) return false;

            // check for current browser
            final var currentBrowser = getDefaultBrowser(pmn, userId);
            if (caller.equals(currentBrowser) || query.equals(currentBrowser)) return false;
        }

        if (appConfig.getExtraAppList().contains(query)) return !appConfig.getUseWhitelist();
        if (appConfig.getExtraOppositeAppList().contains(query)) return appConfig.getUseWhitelist();

        for (var tplName : appConfig.getApplyTemplates()) {
            final var tpl = config.getTemplates().get(tplName);
            if (tpl == null) continue;

            if (tpl.getAppList().contains(query)) {
                if (isAppInGMSIgnoredPackages(caller, query)) return false;

                return !appConfig.getUseWhitelist();
            }
        }

        if (!config.getIgnoredPackagesForPresets().contains(query)) {
            final var appPresets = AppPresets.Companion.getInstance();

            for (var presetName : appConfig.getApplyPresets()) {
                if (appPresets.containsPackage(presetName, query)) {
                    // Do not hide apps from Play Store if they are connected to GMS
                    final var overriddenCaller = Constants.VENDING_PACKAGE_NAME.equals(caller)
                            ? Constants.GMS_PACKAGE_NAME
                            : caller;

                    return !isAppInGMSIgnoredPackages(overriddenCaller, query);
                }
            }
        }

        final var whitelist = appConfig.getUseWhitelist();
        if (whitelist && appConfig.getExcludeSystemApps() && systemApps.contains(query)) {
            return false;
        }

        return whitelist;
    }

    public List<Integer> getRestrictedZygotePermissions(String caller) {
        final var scope = config.getScope().get(caller);
        return scope != null
                ? scope.getRestrictedZygotePermissions()
                : null;
    }

    public boolean shouldHideActivityLaunch(@Nullable String caller, @Nullable String query, int userId) {
        final var appConfig = config.getScope().get(caller);
        if (appConfig != null && shouldHide(caller, query, userId)) {
            return appConfig.getInvertActivityLaunchProtection() == config.getDisableActivityLaunchProtection();
        }

        return false;
    }

    public int shouldHideInstallationSource(@Nullable String caller, @Nullable String query, int callingUser) {
        if (caller == null || query == null) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED;
        if (BuildConfig.APP_PACKAGE_NAME.equals(caller)) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED;

        final var appConfig = config.getScope().get(caller);
        if (appConfig == null || !appConfig.getHideInstallationSource())
            return Constants.FAKE_INSTALLATION_SOURCE_DISABLED;

        logD(TAG, null, () -> "@shouldHideInstallationSource " + caller + ": " + query);

        if (caller.equals(query) && appConfig.getExcludeTargetInstallationSource())
            return Constants.FAKE_INSTALLATION_SOURCE_DISABLED;

        try {
            final var installed = pms.isPackageAvailable(query, callingUser);
            logD(TAG, null, () -> "@shouldHideInstallationSource UID for " + caller + ", " + callingUser + ": " + query + ", " + installed);
            if (!installed) return Constants.FAKE_INSTALLATION_SOURCE_DISABLED; // invalid package installation source request
        } catch (Throwable e) {
            logD(TAG, e, () -> "@shouldHideInstallationSource UID error for " + caller + ", " + callingUser);
            return Constants.FAKE_INSTALLATION_SOURCE_DISABLED;
        }

        return systemApps.contains(query)
                ? appConfig.getHideSystemInstallationSource()
                    ? Constants.FAKE_INSTALLATION_SOURCE_SYSTEM
                    : Constants.FAKE_INSTALLATION_SOURCE_DISABLED
                : Constants.FAKE_INSTALLATION_SOURCE_USER;
    }

    private boolean isManagerWorkModeNotOK(boolean silent) {
        if (managerWorkMode != Constants.MANAGER_WORK_MODE_OK) {
            if (!silent) logW(TAG, null, () -> "Cannot write while in no hooks mode");
            return true;
        }

        return false;
    }

    public void addLog(String parsedMsg) throws IOException, RemoteException {
        if (isManagerWorkModeNotOK(true)) return;

        synchronized (loggerLock) {
            if (!logcatAvailable) return;
            if (logFile.length() / 1024 > config.getMaxLogSize()) clearLogs();
            appendText(logFile, parsedMsg);
        }
    }

    public void writeConfig(String json) {
        if (isManagerWorkModeNotOK(false)) return;

        synchronized (configLock) {
            try {
                final var newConfig = JsonConfig.Companion.parse(json);
                cleanRemnantsFromConfig(newConfig);
                config = newConfig;
                ensureFileIsRW(configFile, true);
                writeText(configFile, json);
                dataHolder.clearUidCache();

                // remove filter counts for apps if they are not in config
                CollectionUtils.removeIf(
                        dataHolder.getFilterHolder().getFilterCounts(),
                        (key, value) -> config.getScope().containsKey(key)
                );

                logD(TAG, null, () -> "Config synced");
            } catch (Throwable e) {
                logE(TAG, e, () -> "An error occurred while writing config");
            }
        }

        writeFilterCount(true);
    }

    private void writeFilterCount() {
        writeFilterCount(false);
    }

    private void writeFilterCount(boolean force) {
        if (isManagerWorkModeNotOK(false)) return;

        synchronized (configLock) {
            if (!force && dataHolder.getFilterHolder().getTotalCount() % 100 != 0) {
                return;
            }

            try {
                ensureFileIsRW(filterCountFile, true);
                writeText(filterCountFile, getDetailedFilterStats());

                logD(TAG, null, () -> "Filter count synced");
            } catch (Throwable e) {
                logE(TAG, e, () -> "An error occurred while writing filter count");
            }
        }
    }

    @Override
    public int getServiceVersion() throws RemoteException {
        return BuildConfig.SERVICE_VERSION;
    }

    @Override
    public int getFilterCount() throws RemoteException {
        return dataHolder.getFilterHolder().getTotalCount();
    }

    @SuppressWarnings("all")
    @Override
    public void clearLogs() throws RemoteException {
        if (isManagerWorkModeNotOK(false)) return;

        synchronized (loggerLock) {
            oldLogFile.delete();
            logFile.renameTo(oldLogFile);

            try {
                logFile.createNewFile();
            } catch (IOException e) {
                throw new RemoteException(e.getMessage());
            }
        }
    }

    @Override
    public void handlePackageEvent(String eventType, String packageName, @Nullable Bundle extras) throws RemoteException {
        final var appPresets = AppPresets.Companion.getInstance();

        switch (eventType) {
            case Intent.ACTION_PACKAGE_ADDED -> {
                if (BuildConfig.APP_PACKAGE_NAME.equals(packageName) && appUid < 0) {
                    appUid = findAndVerifyAppSignature(pms);
                }

                /*
                 * - Ignore when the default config was not available
                 * - Ignore for the manager app
                 * - Ignore for the package updates
                 * - Ignore when the target app had a config
                 */
                final var isDefConfigEnabled = config.getDefaultConfig() != null &&
                        !BuildConfig.APP_PACKAGE_NAME.equals(packageName) &&
                        (extras == null || !extras.getBoolean(Intent.EXTRA_REPLACING)) &&
                        config.getScope().putIfAbsent(packageName, config.getDefaultConfig()) == null;

                if (isDefConfigEnabled) {
                    writeConfig(config.toString());
                }

                // Handle app presets
                appPresets.handlePackageAdded(pms, packageName, preset -> {
                    if (dataHolder.addIntoPresetCache(preset, packageName)) {
                        writePresetCache();
                    }

                    // noinspection all
                    return null;
                });
            }
            case Intent.ACTION_PACKAGE_REMOVED -> {
                // ignore package updates
                if (extras != null && extras.getBoolean(Intent.EXTRA_REPLACING)) {
                    return;
                }

                if (BuildConfig.APP_PACKAGE_NAME.equals(packageName) && appUid >= 0) {
                    logI(TAG, null, () -> "The manager app is uninstalled, looking for alternatives");

                    appUid = findAndVerifyAppSignature(pms);
                }

                // Handle app presets if the app is removed entirely
                if (!findApp(pms, packageName)) {
                    final var removedFromPresets = new AtomicBoolean(false);

                    appPresets.handlePackageRemoved(packageName, preset -> {
                        if (dataHolder.removeFromPresetCache(preset, packageName)) {
                            removedFromPresets.set(true);
                        }

                        // noinspection all
                        return null;
                    });

                    if (removedFromPresets.get()) {
                        writePresetCache();
                    }
                }
            }
        }
    }

    @Override
    public String[] getPackagesForPreset(String presetName) throws RemoteException {
        final var appPresets = AppPresets.Companion.getInstance();
        final var preset = appPresets.getPresetByName(presetName);
        if (preset == null) return new String[0];
        return preset.getPackages().toArray(new String[0]);
    }

    @Override
    public void forceStop(String packageName, int userId) throws RemoteException {
        binderLocalScopeNoReturn(() -> {
            try {
                ActivityManagerUtils.forceStopPackage(packageName, userId);
            } catch (Throwable e) {
                logE(TAG, e, () -> "An error occurred while force stopping the package");
            }
        });
    }

    @Override
    public void log(int level, String tag, String message) throws RemoteException {
        logWithLevel(level, tag, null, () -> message);
    }

    @Override
    public String[] getPackageNames(int userId) throws RemoteException {
        return binderLocalScope(() -> {
            final var list = new HashSet<String>();

            try {
                for (String packageName : pms.getAllPackages()) {
                    try {
                        if (pms.isPackageAvailable(packageName, userId)) {
                            list.add(packageName);
                        }
                    } catch (Throwable ignore) {}
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            return list;
        }).toArray(new String[0]);
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int userId) throws RemoteException {
        return binderLocalScope(() -> Utils.getPackageInfoCompat(pms, packageName, 0, userId));
    }

    @Override
    public String[] listAllSettings(String databaseName) throws RemoteException {
        final Class<?> clazz = switch (databaseName) {
            case Constants.SETTINGS_GLOBAL -> Settings.Global.class;
            case Constants.SETTINGS_SECURE -> Settings.Secure.class;
            case Constants.SETTINGS_SYSTEM -> Settings.System.class;
            default -> throw new RemoteException("Invalid database name " + databaseName);
        };

        final var list = new ArrayList<String>();
        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    list.add((String) field.get(null));
                } catch (Throwable ignore) {}
            }
        }
        Collections.sort(list);

        return list.toArray(new String[0]);
    }

    @Override
    public String getLogFileLocation() throws RemoteException {
        return logFile.getAbsolutePath();
    }

    private void reloadPresets(boolean fromScratch) {
        logI(TAG, null, () -> "Reloading presets " + (fromScratch ? "from scratch" : "over cache"));

        final var apps = binderLocalScope(() -> {
            final var retList = new ArrayList<ApplicationInfo>();

            try {
                for (var id : UserManagerUtils.getUserIds()) {
                    retList.addAll(getInstalledApplicationsCompat(pms, 0, id));
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            return retList;
        });

        final var appPresets = AppPresets.Companion.getInstance();
        appPresets.reloadPresets(apps, fromScratch);
        logI(TAG, null, () -> "All presets are loaded");

        dataHolder.setPresetCache(appPresets.exportCache());

        writePresetCache();
    }

    private void writePresetCache() {
        try {
            writeText(presetCacheFileNew, dataHolder.getPresetCache().toString());
        } catch (Throwable e) {
            logE(TAG, e, () -> "Failed to write into preset cache file");
        }
    }

    @Override
    public void reloadPresetsFromScratch() throws RemoteException {
        reloadPresets(true);
    }

    @Override
    public String getDetailedFilterStats() throws RemoteException {
        return dataHolder.getPresetCache().toString();
    }

    @Override
    public void clearFilterStats() throws RemoteException {
        synchronized (configLock) {
            dataHolder.getFilterHolder().getFilterCounts().clear();
        }

        writeFilterCount(true);
    }

    @Override
    public String getServiceVersionName() throws RemoteException {
        return BuildConfig.APP_VERSION_NAME;
    }

    @Override
    public String[] getLoadedHooks() throws RemoteException {
        final var hookList = new ArrayList<String>();

        for (var entry : hooker.hooks.entrySet()) {
            for (var element : entry.getValue()) {
                hookList.add(new JsonConfig.HookItem(
                        entry.getKey(),
                        element.methodName,
                        element.argumentCount
                ).toString());
            }
        }

        return hookList.toArray(new String[0]);
    }

    @Override
    public ParcelFileDescriptor readFD(int type) throws RemoteException {
        switch (type) {
            case Constants.PARCEL_TYPE_LOG -> {
                ensureFileIsRW(logFile, false);
                try {
                    return ParcelFileDescriptor.open(logFile, ParcelFileDescriptor.MODE_READ_ONLY);
                } catch (FileNotFoundException e) {
                    throw new RemoteException(e.getMessage());
                }
            }
            case Constants.PARCEL_TYPE_CONFIG -> {
                try {
                    return ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY);
                } catch (FileNotFoundException e) {
                    throw new RemoteException(e.getMessage());
                }
            }
            default -> throw new RemoteException("Invalid type for read: " + type);
        }
    }

    @Override
    public void writeFD(int type, ParcelFileDescriptor fd) throws RemoteException {
        if (type == Constants.PARCEL_TYPE_CONFIG) {
            try (var stream = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                writeConfig(readStream(stream));
            } catch (IOException e) {
                throw new RemoteException(e.getMessage());
            }
        } else {
            throw new RemoteException("Invalid type for write: " + type);
        }
    }

    @Override
    public int getManagerWorkMode() {
        return managerWorkMode;
    }

    @Override
    public void startMainActivityAsUser(String packageName, int userId) throws RemoteException {
        final var packageInfo = Utils.getPackageInfoCompat(pms, packageName, 0, userId);
        if (packageInfo == null) throw new RemoteException("Cannot find package info for " + packageName);

        if (packageInfo.applicationInfo != null && packageInfo.applicationInfo.enabled) {
            final var intent = getLaunchIntentForPackageAsUser(packageName, userId);
            if (intent != null) {
                ActivityManagerUtils.startActivity(intent, userId);
            } else {
                throw new RemoteException("No main activity found to launch this app");
            }
        } else {
            throw new RemoteException(packageName + " is disabled");
        }
    }

    @Override
    public boolean migrateData(String packageName) throws RemoteException {
        if (!conflictedModules.contains(packageName)) return false;

        final var dataFile = getDataFile(packageName);
        if (dataFile == null) return false;

        try {
            writeText(configFile, readText(dataFile));

            return true;
        } catch (Throwable e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void reloadConfigFromFile() throws RemoteException {
        try {
            config = JsonConfig.Companion.parse(readText(configFile));
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public int[] getUserProfiles() throws RemoteException {
        return binderLocalScope(() -> {
            try {
                return UserManagerUtils.getUserIds();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("SdCardPath")
    @Nullable
    private File getDataFile(String packageName) {
        // Android 11+
        final var dataMirror = new File(String.format("/data_mirror/data_ce/null/0/%s/files/config.json", packageName));
        if (dataMirror.exists()) return dataMirror;

        // Android 10-
        final var data = new File(String.format("/data/data/%s/files/config.json", packageName));
        if (data.exists()) return data;

        return null;
    }
}
