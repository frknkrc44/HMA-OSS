package org.frknkrc44.hma_oss.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dev.androidbroadcast.vbpd.viewBinding
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.fragment.ScopeFragmentArgs
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentBulkConfigWizardBinding
import org.frknkrc44.hma_oss.ui.viewmodel.BulkConfigWizardViewModel

class BulkConfigWizardFragment : Fragment(R.layout.fragment_bulk_config_wizard) {

    private val binding by viewBinding(FragmentBulkConfigWizardBinding::bind)
    private val viewModel by viewModels<BulkConfigWizardViewModel> {
        BulkConfigWizardViewModel.Factory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_bulk_config_wizard),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() },
        )

        binding.appliedApps.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.appliedAppList.value = bundle.getStringArrayList("checked")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = false,
                checked = viewModel.appliedAppList.value.toTypedArray()
            )
            navigate(R.id.nav_scope, args.toBundle())
        }
        binding.targetAppSettings.setOnClickListener {
            setFragmentResultListener("bulk_app_settings") { _, bundle ->
                val appConfigStr = bundle.getString("appConfig")
                viewModel.appConfig.value = if (appConfigStr != null) JsonConfig.AppConfig.parse(appConfigStr) else null
                clearFragmentResultListener("bulk_app_settings")
            }
            val args = AppSettingsV2FragmentArgs(
                packageName = "bulk_config",
                bulkConfig = viewModel.appConfig.value?.toString(),
                bulkConfigMode = true,
                bulkConfigApps = viewModel.appliedAppList.value.toTypedArray(),
            )
            navigate(R.id.nav_app_settings, args.toBundle())
        }
        with(binding.applyButton){
            if (PrefManager.systemWallpaper) {
                background.alpha = 0xAA
            }

            setOnClickListener {
                if (viewModel.appliedAppList.value.isEmpty()) return@setOnClickListener

                for (pkg in viewModel.appliedAppList.value) {
                    ConfigManager.setAppConfig(pkg, viewModel.appConfig.value)
                }

                showToast(android.R.string.ok)
                navController.navigateUp()
            }
        }

        lifecycleScope.launch {
            viewModel.appliedAppList.collect {
                binding.appliedApps.text = String.format(getString(R.string.template_applied_count), it.size)
            }
        }

        lifecycleScope.launch {
            viewModel.appConfig.collect {
                binding.targetAppSettings.subText = getString(
                    if (it != null) R.string.enabled
                    else R.string.disabled
                )
            }
        }

        setEdge2EdgeFlags(binding.root)
    }
}
