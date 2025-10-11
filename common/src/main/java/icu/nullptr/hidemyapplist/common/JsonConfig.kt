package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.frknkrc44.hma_oss.common.BuildConfig

private val encoder = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
data class JsonConfig(
    var configVersion: Int = BuildConfig.CONFIG_VERSION,
    var detailLog: Boolean = false,
    var maxLogSize: Int = 512,
    var forceMountData: Boolean = true,
    var disableActivityLaunchProtection: Boolean = false,
    var altAppDataIsolation: Boolean = false,
    var altVoldAppDataIsolation: Boolean = false,
    var skipSystemAppDataIsolation: Boolean = true,
    val templates: MutableMap<String, Template> = mutableMapOf(),
    val scope: MutableMap<String, AppConfig> = mutableMapOf()
) {
    @Serializable
    data class Template(
        val isWhitelist: Boolean,
        val appList: Set<String>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    data class AppConfig(
        var useWhitelist: Boolean = false,
        var excludeSystemApps: Boolean = true,
        var hideInstallationSource: Boolean = false,
        var hideSystemInstallationSource: Boolean = false,
        var excludeTargetInstallationSource: Boolean = false,
        var invertActivityLaunchProtection: Boolean = false,
        var applyTemplates: MutableSet<String> = mutableSetOf(),
        var applyPresets: MutableSet<String> = mutableSetOf(),
        var applySettingsPresets: MutableSet<String> = mutableSetOf(),
        var extraAppList: MutableSet<String> = mutableSetOf()
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<JsonConfig>(json)
    }

    override fun toString() = encoder.encodeToString(this)
}
