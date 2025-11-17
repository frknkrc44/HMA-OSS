package org.frknkrc44.hma_oss.ui.adapter

import android.os.Bundle
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import org.frknkrc44.hma_oss.ui.util.targetSettingListToBundle
import org.frknkrc44.hma_oss.ui.util.toTargetSettingList

class SettingsTemplateListAdapter(
    items: Bundle,
    private val onItemClickListener: (SettingsTemplateListAdapter, ReplacementItem) -> Unit
) : BaseSettingsPTAdapter() {
    override val items by lazy {
        val list = mutableListOf<ReplacementItem>()
        list.addAll(items.toTargetSettingList())
        return@lazy list
    }

    fun targetSettingListToBundle() = items.targetSettingListToBundle()

    override fun onItemClick(item: ReplacementItem) = onItemClickListener(this, item)
}