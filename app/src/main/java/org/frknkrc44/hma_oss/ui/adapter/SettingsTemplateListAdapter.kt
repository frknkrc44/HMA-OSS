package org.frknkrc44.hma_oss.ui.adapter

import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.service.ConfigManager
import org.frknkrc44.hma_oss.ui.util.targetSettingListToBundle

class SettingsTemplateListAdapter(name: String?, private val onItemClickListener: (SettingsTemplateListAdapter, ReplacementItem) -> Unit) : BaseSettingsPTAdapter(name) {
    override val items by lazy {
        val list = mutableListOf<ReplacementItem>()
        if (!name.isNullOrEmpty()) {
            list.addAll(ConfigManager.getSettingTemplateTargetSettingList(name))
        }

        return@lazy list
    }

    fun targetSettingListToBundle() = items.targetSettingListToBundle()

    override fun onItemClick(item: ReplacementItem) {
        onItemClickListener(this, item)
    }
}