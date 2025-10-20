package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentSettingsPresetBinding

/**
 * Use the [SettingsPresetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsPresetFragment : Fragment(R.layout.fragment_settings_preset) {
    private val binding by viewBinding<FragmentSettingsPresetBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar(
            toolbar = binding.toolbar,
            title = requireArguments().getString(ARG_PRESET_TITLE)!!
        )


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