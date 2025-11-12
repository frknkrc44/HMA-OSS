package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import org.frknkrc44.hma_oss.ui.adapter.SettingsPresetListAdapter

/**
 * Use the [SettingsPresetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsPresetFragment : BaseSettingsPTFragment() {
    private val args by lazy { navArgs<SettingsPresetFragmentArgs>() }

    override val adapter by lazy { SettingsPresetListAdapter(args.value.name) }

    override val title by lazy { args.value.title }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override val menu = null

    companion object {
        private const val ARG_PRESET_NAME = "name"
        private const val ARG_PRESET_TITLE = "title"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param name Name of the preset.
         * @param title Title of the preset.
         * @return A new instance of fragment SettingsPresetFragment.
         */
        @JvmStatic
        fun newInstance(name: String, title: String) =
            SettingsPresetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRESET_NAME, name)
                    putString(ARG_PRESET_TITLE, title)
                }
            }
    }
}