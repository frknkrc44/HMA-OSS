package top.secret.hma.v1.ui.adapter

import top.secret.hma.v1.common.SettingsPresets
import top.secret.hma.v1.common.settings_presets.ReplacementItem

class SettingsPresetListAdapter(name: String) : BaseSettingsPTAdapter() {
    override val items by lazy {
        SettingsPresets.instance.getPresetByName(name)!!.settingsKVPairs.sortedBy { it.name }
    }

    override fun onItemClick(item: ReplacementItem) {
        // do nothing
    }
}
