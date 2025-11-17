package org.frknkrc44.hma_oss.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentPresetManageBinding
import org.frknkrc44.hma_oss.ui.adapter.AppPresetListAdapter

class PresetManageFragment : Fragment(R.layout.fragment_preset_manage) {

    private val binding by viewBinding<FragmentPresetManageBinding>()
    private val adapter by lazy {
        AppPresetListAdapter(requireContext(), this::navigateToPreset)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_preset_manage),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() }
        )
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.presetList.layoutManager = LinearLayoutManager(context)
        binding.presetList.adapter = adapter

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

    private fun navigateToPreset(presetInfo: ConfigManager.PresetInfo) {
        when (presetInfo.type!!) {
            ConfigManager.PTType.APP -> {
                val args = AppPresetFragmentArgs(presetInfo.name, presetInfo.translation)
                navigate(R.id.nav_preset_inner_manage, args.toBundle())
            }
            ConfigManager.PTType.SETTINGS -> {
                val args = SettingsPresetFragmentArgs(presetInfo.name, presetInfo.translation)
                navigate(R.id.nav_settings_preset_inner_manage, args.toBundle())
            }
        }
    }
}
