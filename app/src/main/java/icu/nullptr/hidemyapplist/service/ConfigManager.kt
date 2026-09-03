package icu.nullptr.hidemyapplist.service

import android.os.Build
import android.util.Log
import icu.nullptr.hidemyapplist.common.CollectionUtils.removeIfWithCount
import icu.nullptr.hidemyapplist.common.CollectionUtils.sync
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.service.ServiceClient.log
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.util.PackageHelper
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.common.BuildConfig

object ConfigManager {
    /**
     * Indicates the type of preset/template.
     *
     * @see APP
     * @see SETTINGS
     */
    enum class PTType {
        /**
         * This preset/template type is used for app filtering.
         */
        APP,

        /**
         * This preset/template type is used for settings filtering.
         */
        SETTINGS,

        /**
         * Ignored apps from presets, not really a preset
         */
        IGNORED_APPS,
    }

    data class TemplateInfo(val name: String?, val type: PTType, val isWhiteList: Boolean)
    data class PresetInfo(val name: String, val type: PTType?, val translation: String)

    private const val TAG = "ConfigManager"
    private var config = JsonConfig()

    fun init() {
        try {
            val rawConfig = ServiceClient.config
            config = JsonConfig.parse(rawConfig)
        } catch (_: Throwable) {
            // ignore the issues
        }

        config.configVersion = BuildConfig.CONFIG_VERSION
    }

    fun saveConfig() {
        ServiceClient.config = config.toString()
    }

    var detailLog: Boolean
        get() = config.detailLog
        set(value) {
            config.detailLog = value
            saveConfig()
        }

    var errorOnlyLog: Boolean
        get() = config.errorOnlyLog
        set(value) {
            config.errorOnlyLog = value
            saveConfig()
        }

    var maxLogSize: Int
        get() = config.maxLogSize
        set(value) {
            config.maxLogSize = value
            saveConfig()
        }

    var forceMountData: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) config.forceMountData
            else false
        set(value) {
            config.forceMountData = value
            saveConfig()
        }

    var disableActivityLaunchProtection: Boolean
        get() = config.disableActivityLaunchProtection
        set(value) {
            config.disableActivityLaunchProtection = value
            saveConfig()
        }

    var altAppDataIsolation: Boolean
        get() = config.altAppDataIsolation
        set(value) {
            config.altAppDataIsolation = value
            saveConfig()
        }

    var altVoldAppDataIsolation: Boolean
        get() = config.altVoldAppDataIsolation
        set(value) {
            config.altVoldAppDataIsolation = value
            saveConfig()
        }

    var skipSystemAppDataIsolation: Boolean
        get() = config.skipSystemAppDataIsolation
        set(value) {
            config.skipSystemAppDataIsolation = value
            saveConfig()
        }

    var webViewProtection: Boolean
        get() = config.webViewProtection
        set(value) {
            config.webViewProtection = value
            saveConfig()
        }

    var defaultConfig: JsonConfig.AppConfig?
        get() = config.defaultConfig
        set(value) {
            config.defaultConfig = value
            saveConfig()
        }

    var disabledHooks: List<JsonConfig.HookItem>
        get() = config.disabledHooks
        set(elements) {
            config.disabledHooks.sync(elements)
            saveConfig()
            showToast(R.string.settings_need_reboot)
        }

    var ignoredPackagesForPresets: Set<String>
        get() = config.ignoredPackagesForPresets
        set(elements) {
            config.ignoredPackagesForPresets.sync(elements)
            saveConfig()
        }

    fun importConfig(json: String) {
        config = JsonConfig.parse(json)
        config.configVersion = BuildConfig.CONFIG_VERSION
        saveConfig()
    }

    fun hasTemplate(name: String?): Boolean {
        return config.templates.containsKey(name)
    }

    fun getTemplateList(): MutableList<TemplateInfo> {
        return config.templates.mapTo(mutableListOf()) { TemplateInfo(it.key, PTType.APP, it.value.isWhitelist) }
    }

    fun getTemplateAppliedAppList(name: String): ArrayList<String> {
        return config.scope.mapNotNullTo(ArrayList()) {
            if (it.value.applyTemplates.contains(name)) it.key else null
        }
    }

    fun getTemplateTargetAppList(name: String): ArrayList<String> {
        return ArrayList(config.templates[name]?.appList ?: emptyList())
    }

    fun deleteTemplate(name: String) {
        config.scope.forEach { (_, appInfo) ->
            appInfo.applyTemplates.remove(name)
        }
        config.templates.remove(name)
        saveConfig()
    }

    fun renameTemplate(oldName: String, newName: String) {
        if (oldName == newName) return
        config.scope.forEach { (_, appInfo) ->
            if (appInfo.applyTemplates.contains(oldName)) {
                appInfo.applyTemplates.remove(oldName)
                appInfo.applyTemplates.add(newName)
            }
        }
        config.templates[newName] = config.templates[oldName]!!
        config.templates.remove(oldName)
        saveConfig()
    }

    fun updateTemplate(name: String, template: JsonConfig.Template) {
        log(Log.DEBUG, TAG, "updateTemplate: $name list = ${template.appList}")
        config.templates[name] = template
        saveConfig()
    }

    fun updateTemplateAppliedApps(name: String, appliedList: List<String>) {
        log(Log.DEBUG, TAG, "updateTemplateAppliedApps: $name list = $appliedList")
        config.scope.forEach { (app, appInfo) ->
            if (appliedList.contains(app)) appInfo.applyTemplates.add(name)
            else appInfo.applyTemplates.remove(name)
        }
        saveConfig()
    }

    fun getSettingTemplateList(): MutableList<TemplateInfo> {
        return config.settingsTemplates.mapTo(mutableListOf()) { TemplateInfo(it.key, PTType.SETTINGS, false) }
    }

    fun getSettingTemplateAppliedAppList(name: String): ArrayList<String> {
        return config.scope.mapNotNullTo(ArrayList()) {
            if (it.value.applySettingTemplates.contains(name)) it.key else null
        }
    }

    fun getSettingTemplateTargetSettingList(name: String): ArrayList<ReplacementItem> {
        return ArrayList(config.settingsTemplates[name]?.settingsList ?: emptyList())
    }

    fun deleteSettingTemplate(name: String) {
        config.scope.forEach { (_, appInfo) ->
            appInfo.applySettingTemplates.remove(name)
        }
        config.settingsTemplates.remove(name)
        saveConfig()
    }

    fun renameSettingTemplate(oldName: String, newName: String) {
        if (oldName == newName) return
        config.scope.forEach { (_, appInfo) ->
            if (appInfo.applySettingTemplates.contains(oldName)) {
                appInfo.applySettingTemplates.remove(oldName)
                appInfo.applySettingTemplates.add(newName)
            }
        }
        config.settingsTemplates[newName] = config.settingsTemplates[oldName]!!
        config.settingsTemplates.remove(oldName)
        saveConfig()
    }

    fun updateSettingTemplate(name: String, template: JsonConfig.SettingsTemplate) {
        log(Log.DEBUG, TAG, "updateSettingTemplate: $name list = ${template.settingsList}")
        config.settingsTemplates[name] = template
        saveConfig()
    }

    fun updateSettingTemplateAppliedApps(name: String, appliedList: List<String>) {
        log(Log.DEBUG, TAG, "updateSettingTemplateAppliedApps: $name list = $appliedList")
        config.scope.forEach { (app, appInfo) ->
            if (appliedList.contains(app)) appInfo.applySettingTemplates.add(name)
            else appInfo.applySettingTemplates.remove(name)
        }
        saveConfig()
    }

    fun isHideEnabled(packageName: String): Boolean {
        return config.scope.containsKey(packageName)
    }

    fun getAppConfig(packageName: String): JsonConfig.AppConfig? {
        return config.scope[packageName]
    }

    fun setAppConfig(packageName: String, appConfig: JsonConfig.AppConfig?) {
        if (appConfig == null) config.scope.remove(packageName)
        else config.scope[packageName] = appConfig
        saveConfig()
    }

    fun clearUninstalledAppConfigs(inConfig: JsonConfig = config, onFinish: (success: Boolean) -> Unit) {
        PackageHelper.invalidateCache { throwable ->
            if (throwable == null) {
                // --- STEP 1: Clear uninstalled app configs ---
                val scopeRemoveCount = inConfig.scope.removeIfWithCount { packageName, _ ->
                    !PackageHelper.exists(packageName)
                }

                // --- STEP 2: Clear uninstalled apps from extra app lists ---
                var cleanedAppCount = 0
                inConfig.scope.values.forEach { config ->
                    cleanedAppCount += config.extraAppList.removeIfWithCount { packageName ->
                        !PackageHelper.exists(packageName)
                    }

                    cleanedAppCount += config.extraOppositeAppList.removeIfWithCount { packageName ->
                        !PackageHelper.exists(packageName)
                    }
                }

                // --- STEP 3: Clear uninstalled apps from templates ---
                inConfig.templates.forEach { (key, value) ->
                    val newList = value.appList.filter { PackageHelper.exists(it) }
                    val count = value.appList.size - newList.size

                    if (count > 0) {
                        cleanedAppCount += count
                        inConfig.templates[key] = JsonConfig.Template(
                            isWhitelist = value.isWhitelist,
                            appList = newList.toSet(),
                        )
                    }
                }

                if ((scopeRemoveCount > 0 || cleanedAppCount > 0) && inConfig == config) {
                    log(Log.INFO, TAG, "Pruned $scopeRemoveCount app configs and $cleanedAppCount app entries")
                    saveConfig()
                }

                onFinish(true)
            } else {
                onFinish(false)
            }
        }
    }

    fun getRawConfig(deepCopy: Boolean): JsonConfig {
        if (deepCopy) {
            val scopeCopy = config.scope.toMutableMap()
            val templateCopy = config.templates.toMutableMap()
            val settingsTemplateCopy = config.settingsTemplates.toMutableMap()

            return config.copy(
                scope = scopeCopy,
                templates = templateCopy,
                settingsTemplates = settingsTemplateCopy,
            )
        }

        return config
    }

    fun resetConfig() {
        config = JsonConfig()
        saveConfig()
    }
}
