package org.frknkrc44.hma_oss.ui.fragment

import android.animation.Animator
import android.animation.AnimatorInflater
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentLogsBinding
import org.frknkrc44.hma_oss.ui.adapter.StatAdapter
import kotlin.concurrent.thread

class StatsFragment : Fragment(R.layout.fragment_logs) {

    private val binding by viewBinding<FragmentLogsBinding>()
    private val adapter by lazy { StatAdapter() }
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

                val countsKeys = stats.filterCounts.keys.sortedWith { key1, key2 ->
                    if (getTotalCount(key1) > getTotalCount(key2)) -1 else 0
                }

                for (key in countsKeys) {
                    adapter.addOrUpdateEntry(
                        key,
                        stats.filterCounts[key]!!
                    )
                }

                adapter.clearEntriesIfNotFound(countsKeys)
            }
        }
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_refresh -> updateLogs()
            R.id.menu_delete -> {
                ServiceClient.clearFilterStats()
                showToast(android.R.string.ok)
                updateLogs()
            }
            // TODO: Add other options when required
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.toolbar) {
            setupToolbar(
                toolbar = this,
                title = getString(R.string.title_filter_logs),
                menuRes = R.menu.menu_stats,
                onMenuOptionSelected = this@StatsFragment::onMenuOptionSelected,
            )
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { navController.popBackStack() }
        }

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        setEdge2EdgeFlags(binding.root)

        updateLogs()
    }
}
