package top.secret.hma.v1.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.androidbroadcast.vbpd.viewBinding
import top.secret.hma.v1.hmaApp
import top.secret.hma.v1.service.ConfigManager
import top.secret.hma.v1.service.ServiceClient
import top.secret.hma.v1.ui.util.navController
import top.secret.hma.v1.ui.util.navigate
import top.secret.hma.v1.ui.util.setEdge2EdgeFlags
import top.secret.hma.v1.ui.util.setupToolbar
import kotlinx.coroutines.launch
import top.secret.hma.v1.R
import top.secret.hma.v1.databinding.FragmentPresetManageBinding
import top.secret.hma.v1.ui.adapter.AppPresetListAdapter

class PresetManageFragment : Fragment(R.layout.fragment_preset_manage) {

    private val binding by viewBinding(FragmentPresetManageBinding::bind)
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
