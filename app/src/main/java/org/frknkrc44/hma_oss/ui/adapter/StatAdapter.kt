package org.frknkrc44.hma_oss.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.util.PackageHelper
import org.frknkrc44.hma_oss.databinding.StatItemViewBinding

class StatAdapter() : RecyclerView.Adapter<StatAdapter.ViewHolder>() {

    class StatItem(
        val packageName: String,
        val filterCount: FilterHolder.FilterCount,
    )

    var logs = listOf<StatItem>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(private val binding: StatItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: StatItem) {
            binding.icon.setImageDrawable(PackageHelper.loadAppIcon(logItem.packageName))
            binding.tag.text = PackageHelper.loadAppLabel(logItem.packageName)
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
