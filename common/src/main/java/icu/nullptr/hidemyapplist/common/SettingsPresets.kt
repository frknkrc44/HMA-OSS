package icu.nullptr.hidemyapplist.common

import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset
import icu.nullptr.hidemyapplist.common.settings_presets.BasePreset
import icu.nullptr.hidemyapplist.common.settings_presets.DeveloperOptionsPreset
import icu.nullptr.hidemyapplist.common.settings_presets.InputMethodPreset

class SettingsPresets private constructor() {
    private val presetList = mutableListOf<BasePreset>()

    companion object {
        val instance by lazy { SettingsPresets() }
    }

    fun getAllPresetNames() = presetList.map { it.name }.toTypedArray()
    fun getPresetByName(name: String) = presetList.firstOrNull { it.name == name }

    init {
        presetList.add(DeveloperOptionsPreset())
        presetList.add(AccessibilityPreset())
        presetList.add(InputMethodPreset())
    }
}
