package icu.nullptr.hidemyapplist.common

import icu.nullptr.hidemyapplist.common.Constants.ENABLE_INTERNET_UNKNOWN
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.frknkrc44.hma_oss.common.BuildConfig

@Serializable
data class JsonConfig(
    var configVersion: Int = BuildConfig.CONFIG_VERSION,

    /**
     * Enable/disable debug (and verbose for debug builds) logging
     */
    var detailLog: Boolean = false,

    /**
     * Enable/disable error-only logging
     *
     * It will disable all log channels except the error channel
     */
    var errorOnlyLog: Boolean = false,

    /**
     * Maximum log size in KBs.
     *
     * Increasing it much will cause Binder issues for now
     */
    var maxLogSize: Int = 512,

    /**
     * Enable/disable target SDK 30+ restrictions for all apps
     */
    var forceMountData: Boolean = true,

    /**
     * Enable/disable the activity launch protection
     *
     * The activity launch protection basically blocks starting
     * applications when they are hidden from target app, and reduces detections
     */
    var disableActivityLaunchProtection: Boolean = false,

    /**
     * Enable/disable alternative (propless) appdata isolation
     */
    var altAppDataIsolation: Boolean = false,

    /**
     * Enable/disable alternative (propless) vold appdata isolation
     *
     * Use it carefully that some of Android editions are still leaking
     * vold existence even while not using props
     */
    var altVoldAppDataIsolation: Boolean = false,

    /**
     * Skip vold appdata isolation enforcements for system apps
     *
     * This will help you to reduce bootloop issues caused by vold appdata isolation
     */
    var skipSystemAppDataIsolation: Boolean = true,

    /**
     * Use alternative path to query packages
     *
     * This option is useful for querying packages from other profiles,
     * or bypassing some of Chinese OEM ROM (MIUI, HyperOS, ...) restrictions
     * while trying to get package lists in the manager app
     */
    var packageQueryWorkaround: Boolean = false,

    val templates: MutableMap<String, Template> = mutableMapOf(),
    val settingsTemplates: MutableMap<String, SettingsTemplate> = mutableMapOf(),

    /**
     * A package name and config pair to keep per-app configs
     */
    val scope: MutableMap<String, AppConfig> = mutableMapOf()
) {
    @Serializable
    data class Template(
        /**
         * Is it a blacklist or whitelist template?
         */
        val isWhitelist: Boolean,

        /**
         * Apps inside of this application template
         */
        val appList: Set<String>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    /**
     * A type of template that holds settings replacements/overrides
     *
     * Also used for override the settings presets
     */
    data class SettingsTemplate(
        /**
         * Setting replacements inside of this settings template
         */
        val settingsList: Set<ReplacementItem>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    data class AppConfig(
        /**
         * Is it a blacklist or whitelist configuration?
         */
        var useWhitelist: Boolean = false,

        /**
         * Exclude/include system apps for whitelist mode
         */
        var excludeSystemApps: Boolean = true,

        /**
         * Hide all user apps' installation sources
         */
        var hideInstallationSource: Boolean = false,

        /**
         * Hide all system apps' installation sources
         */
        var hideSystemInstallationSource: Boolean = false,

        /**
         * Exclude the target app from installation source replacements
         */
        var excludeTargetInstallationSource: Boolean = false,

        /**
         * Invert the currently active activity protection mode for this application
         *
         * How this option works:
         * - If it was enabled globally, then it will be disabled for this application
         * - If it was disabled globally, then it will be enabled for this application
         */
        var invertActivityLaunchProtection: Boolean = false,

        /**
         * Exclude this app from appdata isolation
         *
         * This option is only useful when you don't need vold appdata isolation
         * for the target app or the app assumes the vold isolation as a detection point
         */
        var excludeVoldIsolation: Boolean = false,

        /**
         * A list of removed Zygote permissions while the app was launching
         */
        var restrictedZygotePermissions: List<Int> = listOf(),

        /**
         * A list of applied application templates, it has to contain items
         * from the global application template list
         */
        var applyTemplates: MutableSet<String> = mutableSetOf(),

        /**
         * A list of applied application presets, it has to contain items
         * from the global application preset list
         */
        var applyPresets: MutableSet<String> = mutableSetOf(),

        /**
         * A list of applied settings templates, it has to contain items
         * from the global settings template list
         */
        var applySettingTemplates: MutableSet<String> = mutableSetOf(),

        /**
         * A list of applied settings presets, it has to contain items
         * from the global settings preset list
         */
        var applySettingsPresets: MutableSet<String> = mutableSetOf(),

        /**
         * Extra hidden/unhidden apps list, depends on `useWhitelist`
         */
        var extraAppList: MutableSet<String> = mutableSetOf(),

        /**
         * Extra hidden/unhidden apps list in opposite way, depends on `useWhitelist`
         *
         * - A list of extra hidden apps on whitelist mode
         * - A list of extra unhidden apps on blacklist mode
         */
        var extraOppositeAppList: MutableSet<String> = mutableSetOf(),
    ) {
        override fun toString() = encoder.encodeToString(this)

        companion object {
            fun parse(json: String) = encoder.decodeFromString<AppConfig>(json)
        }
    }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<JsonConfig>(json)

        val encoder = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    override fun toString() = encoder.encodeToString(this)
}
