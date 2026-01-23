package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.adapter.LogAdapter
import icu.nullptr.hidemyapplist.ui.util.contentResolver
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentLogsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class StatsFragment : Fragment(R.layout.fragment_logs) {

    private val binding by viewBinding<FragmentLogsBinding>()
    private val adapter by lazy { LogAdapter(requireContext(), true) }
    private var statCache: String? = null

    private fun updateLogs() {
        lifecycleScope.launch {
            statCache = runCatching { ServiceClient.detailedFilterStats }.getOrNull()
            if (statCache == null) {
                binding.serviceOff.visibility = View.VISIBLE
            } else {
                binding.serviceOff.visibility = View.GONE

                val stats = FilterHolder.parse(statCache!!)

                fun getTotalCount(key: String) = stats.filterCounts[key]!!.totalCount

                val counts = stats.filterCounts.toSortedMap { key1, key2 ->
                    if(getTotalCount(key1) > getTotalCount(key2)) 1 else -1
                }.toMap()
                adapter.logs = buildList {
                    for (key in counts.keys) {
                        val item = counts[key]!!

                        add(LogAdapter.LogItem(
                            level = "DEBUG",
                            tag = PackageHelper.loadAppLabel(key),
                            message = "PkgMgr: ${item.packageManagerCount}, ActLaunch: ${item.activityLaunchCount}",
                            date = "Settings: ${item.settingsCount}, Installer: ${item.installerCount}, Others: ${item.othersCount}",
                        ))
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.toolbar) {
            setupToolbar(
                toolbar = this,
                title = getString(R.string.title_filter_stats),
            )
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { navController.popBackStack() }
        }

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        updateLogs()

        setEdge2EdgeFlags(binding.root)
    }
}
