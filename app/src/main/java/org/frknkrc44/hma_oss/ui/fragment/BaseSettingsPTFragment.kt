package org.frknkrc44.hma_oss.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentSettingsPtBaseBinding
import org.frknkrc44.hma_oss.ui.adapter.BaseSettingsPTAdapter

abstract class BaseSettingsPTFragment : Fragment(R.layout.fragment_settings_pt_base) {
    val binding by viewBinding<FragmentSettingsPtBaseBinding>()

    abstract val adapter: BaseSettingsPTAdapter

    abstract val title: String?

    internal open fun onBack() {
        navController.navigateUp()
    }

    /**
     * first - menuRes, second - onMenuOptionSelected
     */
    abstract val menu: Pair<Int, ((MenuItem) -> Unit)>?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }

        setupToolbar(
            toolbar = binding.toolbar,
            title = title ?: "",
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() },
            menuRes = menu?.first,
            onMenuOptionSelected = menu?.second,
        )

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter

        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(
                    barInsets.left,
                    barInsets.top,
                    barInsets.right,
                    barInsets.bottom,
                )
            } else {
                @Suppress("deprecation")
                v.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }

            insets
        }
    }
}