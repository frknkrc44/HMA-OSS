package org.frknkrc44.hma_oss.ui.adapter

import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem

class SettingsPresetListAdapter(name: String) : BaseSettingsPTAdapter() {
    override val items by lazy {
        SettingsPresets.instance.getPresetByName(name)!!.settingsKVPairs.sortedBy { it.name }
    }

    override fun onItemClick(item: ReplacementItem) {
        // do nothing
    }
}