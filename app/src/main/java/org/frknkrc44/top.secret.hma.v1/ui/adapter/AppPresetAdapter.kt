package org.frknkrc44.hma_oss.ui.adapter

import android.view.ViewGroup
import android.widget.Filter
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.adapter.AppSelectAdapter
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.runBlocking

class AppPresetAdapter(
    private val presetName: String
) : AppSelectAdapter() {
    var packages = mutableListOf<String>()

    fun updateList() {
        packages.clear()
        packages += ServiceClient.getPackagesForPreset(presetName)?.toList() ?: listOf()
    }

    inner class ViewHolder(view: AppItemView) : AppSelectAdapter.ViewHolder(view) {
        override fun bind(packageName: String) {
            (itemView as AppItemView).let {
                it.load(packageName)
                it.alpha = if (!PackageHelper.exists(packageName)) 0.5f else 1.0f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    private inner class PresetFilter : Filter() {

        override fun performFiltering(constraint: CharSequence): FilterResults {
            return runBlocking {
                val constraintLowered = constraint.toString().trim().lowercase()
                val filteredList = packages.filter {
                    if (constraintLowered.isEmpty()) return@filter true

                    if (it.lowercase().contains(constraintLowered)) return@filter true

                    try {
                        val label = PackageHelper.loadAppLabel(it)
                        return@filter label.lowercase().contains(constraintLowered)
                    } catch (e: Throwable) {
                        return@filter false
                    }
                }

                FilterResults().also { it.values = filteredList }
            }
        }

        @Suppress("UNCHECKED_CAST", "NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val values = results.values
            if (values != null) {
                filteredList = values as List<String>
                notifyDataSetChanged()
            }
        }
    }

    private val mFilter = PresetFilter()

    override fun getFilter(): Filter = mFilter
}
