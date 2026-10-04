package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dev.androidbroadcast.vbpd.viewBinding
import icu.nullptr.hidemyapplist.ui.fragment.LogsFragment
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
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
            adapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
                override fun createFragment(index: Int): Fragment {
                    return when (index) {
                        0 -> LogsFragment(binding.loadingIndicator, binding.toolbar)
                        1 -> StatsFragment(binding.loadingIndicator, binding.toolbar)
                        else -> throw UnsupportedOperationException("Invalid tab index: $index")
                    }
                }

                override fun getItemCount() = tabsList.size
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabsList[position]
        }.attach()

        setEdge2EdgeFlags(binding.root)
    }
}
