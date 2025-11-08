package org.frknkrc44.hma_oss.ui.adapter

import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.service.ConfigManager

class SettingsTemplateListAdapter(name: String?, private val onItemClickListener: (ReplacementItem) -> Unit) : BaseSettingsPTAdapter(name) {
    override val items by lazy {
        val list = mutableListOf<ReplacementItem>()
        if (!name.isNullOrEmpty()) {
            list.addAll(ConfigManager.getSettingTemplateTargetSettingList(name))
        }

        return@lazy list
    }

    override fun onItemClick(item: ReplacementItem) {
        onItemClickListener(item)
    }
}