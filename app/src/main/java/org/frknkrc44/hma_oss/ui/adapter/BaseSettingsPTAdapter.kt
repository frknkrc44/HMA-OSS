package org.frknkrc44.hma_oss.ui.adapter

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.ui.view.AppItemView

abstract class BaseSettingsPTAdapter() : RecyclerView.Adapter<BaseSettingsPTAdapter.ViewHolder>() {
    abstract val items: List<ReplacementItem>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, false).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    abstract fun onItemClick(item: ReplacementItem)

    inner class ViewHolder(view: AppItemView) : RecyclerView.ViewHolder(view) {
        fun bind(item: ReplacementItem) {
            (itemView as AppItemView).apply {
                binding.icon.isVisible = false
                binding.label.text = item.name
                binding.enabled.isVisible = true
                binding.enabled.text = item.database
                binding.packageName.text = item.value ?: "null"
                binding.root.setOnClickListener { onItemClick(item) }
            }
        }
    }
}