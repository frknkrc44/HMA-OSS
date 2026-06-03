package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dev.androidbroadcast.vbpd.viewBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.fragment.LogsFragment
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentTabbedLogsBinding

class TabbedLogsFragment : Fragment() {
    private val binding by viewBinding(FragmentTabbedLogsBinding::bind)

    private val tabsList by lazy {
        listOf(
            getString(R.string.title_logs),
            getString(R.string.title_filter_logs),
        )
    }

    private val logsFragment by lazy { LogsFragment(binding.loadingIndicator) }
    private val statsFragment by lazy { StatsFragment() }

    private val pagerAdapter by lazy {
        object : FragmentStateAdapter(parentFragmentManager, lifecycle) {
            override fun createFragment(index: Int): Fragment {
                return when (index) {
                    0 -> logsFragment
                    1 -> statsFragment
                    else -> throw UnsupportedOperationException("Invalid tab index: $index")
                }
            }

            override fun getItemCount() = tabsList.size
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tabbed_logs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.toolbar) {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { navController.popBackStack() }
        }

        with(binding.viewPager) {
            adapter = pagerAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> {
                            setupToolbar(
                                binding.toolbar,
                                title = getString(R.string.title_logs),
                                menuRes = R.menu.menu_logs,
                                onMenuOptionSelected = logsFragment::onMenuOptionSelected,
                            )

                            with(binding.toolbar.menu) {
                                when (PrefManager.logFilter_level) {
                                    0 -> findItem(R.id.menu_filter_debug).isChecked = true
                                    1 -> findItem(R.id.menu_filter_info).isChecked = true
                                    2 -> findItem(R.id.menu_filter_warn).isChecked = true
                                    3 -> findItem(R.id.menu_filter_error).isChecked = true
                                }
                                findItem(R.id.menu_reverse_order).isChecked = PrefManager.logFilter_reverseOrder
                            }
                        }
                        1 -> {
                            setupToolbar(
                                binding.toolbar,
                                title = getString(R.string.title_filter_logs),
                                menuRes = R.menu.menu_stats,
                                onMenuOptionSelected = statsFragment::onMenuOptionSelected,
                            )
                        }
                    }
                }
            })
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabsList[position]
        }.attach()

        setEdge2EdgeFlags(binding.root)
    }
}
