package top.secret.hma.v1.ui.adapter

import android.os.Bundle
import top.secret.hma.v1.common.settings_presets.ReplacementItem
import top.secret.hma.v1.ui.util.targetSettingListToBundle
import top.secret.hma.v1.ui.util.toTargetSettingList

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
