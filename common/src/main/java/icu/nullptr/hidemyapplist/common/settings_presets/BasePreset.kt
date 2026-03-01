package top.secret.hma.v1.common.settings_presets

abstract class BasePreset(val name: String) {
    abstract val settingsKVPairs: List<ReplacementItem>

    fun getSpoofedValue(key: String) = settingsKVPairs.firstOrNull { it.name == key }
}
