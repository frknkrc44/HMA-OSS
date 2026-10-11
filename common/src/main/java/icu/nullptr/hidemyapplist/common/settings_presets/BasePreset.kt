package icu.nullptr.hidemyapplist.common.settings_presets

abstract class BasePreset(val name: String) {
    abstract val settingsKVPairs: List<ReplacementItem>

    fun getSpoofedValue(key: String, database: String) = settingsKVPairs.firstOrNull {
        it.name == key && it.database == database
    }
}
