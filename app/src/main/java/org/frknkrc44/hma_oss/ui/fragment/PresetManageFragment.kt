package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentPresetManageBinding
import org.frknkrc44.hma_oss.ui.adapter.AppPresetListAdapter
import kotlin.concurrent.thread

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
            navigationOnClick = { navController.navigateUp() },
            menuRes = R.menu.menu_preset_manage,
            onMenuOptionSelected = {
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.refresh)
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create()

                progressDialog.show()

                hmaApp.globalScope.launch {
                    ServiceClient.reloadPresetsFromScratch()

                    progressDialog.dismiss()
                }
            },
        )
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.presetList.layoutManager = LinearLayoutManager(context)
        binding.presetList.adapter = adapter

        setEdge2EdgeFlags(binding.root)
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
