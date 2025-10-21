package org.frknkrc44.hma_oss.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentSettingsPresetBinding
import org.frknkrc44.hma_oss.ui.adapter.SettingsPresetListAdapter

/**
 * Use the [SettingsPresetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsPresetFragment : Fragment(R.layout.fragment_settings_preset) {
    private val binding by viewBinding<FragmentSettingsPresetBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args by navArgs<SettingsPresetFragmentArgs>()

        setupToolbar(
            toolbar = binding.toolbar,
            title = args.presetTitle,
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() }
        )

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = SettingsPresetListAdapter(args.presetName)

        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets = insets.getInsets(WindowInsets.Type.systemBars())
                binding.root.setPadding(
                    barInsets.left,
                    barInsets.top,
                    barInsets.right,
                    barInsets.bottom,
                )
            } else {
                @Suppress("deprecation")
                binding.root.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }

            insets
        }
    }

    companion object {
        private const val ARG_PRESET_NAME = "presetName"
        private const val ARG_PRESET_TITLE = "presetTitle"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param presetName Name of the preset.
         * @param presetTitle Title of the preset.
         * @return A new instance of fragment SettingsPresetFragment.
         */
        @JvmStatic
        fun newInstance(presetName: String, presetTitle: String) =
            SettingsPresetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRESET_NAME, presetName)
                    putString(ARG_PRESET_TITLE, presetTitle)
                }
            }
    }
}