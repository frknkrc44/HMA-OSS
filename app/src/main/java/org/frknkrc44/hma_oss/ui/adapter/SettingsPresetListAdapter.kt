package org.frknkrc44.hma_oss.ui.adapter

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import org.frknkrc44.hma_oss.R

class SettingsPresetListAdapter(presetName: String) : RecyclerView.Adapter<SettingsPresetListAdapter.ViewHolder>() {
    private val presetItems by lazy {
        SettingsPresets.instance.getPresetByName(presetName)!!.settingsKVPairs
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(presetItems[position])

    override fun getItemCount() = presetItems.size

    class ViewHolder(view: AppItemView) : RecyclerView.ViewHolder(view) {
        fun bind(item: ReplacementItem) {
            (itemView as AppItemView).apply {
                binding.icon.isVisible = false
                binding.label.text = item.name
                binding.packageName.text = item.value ?: "null"
            }
        }
    }
}