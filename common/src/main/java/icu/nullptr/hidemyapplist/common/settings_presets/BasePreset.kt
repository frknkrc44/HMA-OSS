package icu.nullptr.hidemyapplist.common.settings_presets

abstract class BasePreset(val name: String) {
    companion object {
        const val SETTINGS_GLOBAL = "global"
        const val SETTINGS_SYSTEM = "system"
        const val SETTINGS_SECURE = "secure"
    }

    abstract val settingsKVPairs: List<ReplacementItem>

    fun getSpoofedValue(key: String) = settingsKVPairs.firstOrNull { it.name == key }
}
