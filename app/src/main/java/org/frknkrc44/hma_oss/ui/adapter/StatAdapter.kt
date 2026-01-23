package org.frknkrc44.hma_oss.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.util.PackageHelper
import org.frknkrc44.hma_oss.databinding.StatItemViewBinding

class StatAdapter() : RecyclerView.Adapter<StatAdapter.ViewHolder>() {

    data class StatItem(
        val packageName: String,
        val filterCount: FilterHolder.FilterCount,
    ) {
        val totalCount get() = filterCount.totalCount
    }

    private val logs = mutableListOf<StatItem>()

    private var wasRefreshing = false

    internal fun addOrUpdateEntry(packageName: String, filterCount: FilterHolder.FilterCount) {
        val position = logs.indexOfFirst { it.packageName == packageName }
        if (position < 0) {
            logs.add(StatItem(packageName, filterCount))
            notifyItemInserted(logs.size - 1)
        } else {
            val item = logs[position]
            if (item.totalCount == filterCount.totalCount && !wasRefreshing) return

            logs[position] = StatItem(packageName, filterCount)

            val resort = logs.sortedWith { it1, it2 -> if (it1.totalCount > it2.totalCount) -1 else 0 }
            val newIndex = resort.indexOfFirst { it.packageName == packageName }

            logs.clear()
            logs.addAll(resort)

            notifyItemChanged(position)

            if (newIndex != position) {
                notifyItemChanged(newIndex)
            }
        }
    }

    inner class ViewHolder(private val binding: StatItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: StatItem) {
            if (PackageHelper.refreshing) {
                binding.tag.text = logItem.packageName
                wasRefreshing = true
            } else {
                binding.icon.setImageDrawable(PackageHelper.loadAppIcon(logItem.packageName))
                binding.tag.text = PackageHelper.loadAppLabel(logItem.packageName)
                wasRefreshing = false
            }

            binding.countPkgMgr.text = logItem.filterCount.packageManagerCount.toString()
            binding.countActLaunch.text = logItem.filterCount.activityLaunchCount.toString()
            binding.countSettings.text = logItem.filterCount.settingsCount.toString()
            binding.countInstallers.text = logItem.filterCount.installerCount.toString()
            binding.countOthers.text = logItem.filterCount.othersCount.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = StatItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = logs.size

    override fun getItemId(position: Int) = logs[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(logs[position])
}
