package org.frknkrc44.hma_oss.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.fragment.ScopeFragmentArgs
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentTemplateSettingsBinding
import org.frknkrc44.hma_oss.ui.util.toTargetSettingList
import org.frknkrc44.hma_oss.ui.viewmodel.SettingsTemplateConfViewModel

class SettingsTemplateConfFragment : Fragment(R.layout.fragment_template_settings) {
    private val binding by viewBinding<FragmentTemplateSettingsBinding>()
    private val viewModel by viewModels<SettingsTemplateConfViewModel> {
        val args by navArgs<SettingsTemplateConfFragmentArgs>()
        SettingsTemplateConfViewModel.Factory(args)
    }

    private fun onBack(delete: Boolean) {
        viewModel.name = viewModel.name?.trim()
        if (viewModel.name != viewModel.originalName && (ConfigManager.hasTemplate(viewModel.name) || viewModel.name == null) || delete) {
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (delete) R.string.template_delete_title else R.string.template_name_invalid)
                .setMessage(if (delete) R.string.template_delete else R.string.template_name_already_exist)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    saveResult(delete)
                }
            if (delete) builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        } else {
            saveResult(false)
        }
    }

    private fun saveResult(delete: Boolean) {
        setFragmentResult("settings_template_conf", Bundle().apply {
            putString("name",if (delete) null else viewModel.name)
            putStringArrayList("appliedList", viewModel.appliedAppList.value)
            putBundle("settingList", viewModel.targetSettingListToBundle())
        })

        navController.navigateUp()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack(false) }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_settings_template_conf),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack(false) },
            menuRes = R.menu.menu_delete,
            onMenuOptionSelected = {
                onBack(true)
            }
        )

        binding.templateName.setText(viewModel.name)
        (binding.workMode.parent as View).isVisible = false
        binding.templateName.addTextChangedListener { viewModel.name = it.toString() }
        binding.targetApps.binding.icon.setImageResource(R.drawable.baseline_settings_24)
        binding.targetApps.setOnClickListener {
            setFragmentResultListener("setting_select") { _, bundle ->
                val targetSettings = bundle.toTargetSettingList()
                viewModel.targetSettingList.value = targetSettings as ArrayList<ReplacementItem>
                clearFragmentResultListener("setting_select")
            }
            val args = SettingsTemplateInnerFragmentArgs(viewModel.targetSettingListToBundle())
            navigate(R.id.nav_settings_template_inner_manage, args.toBundle())
        }
        binding.appliedApps.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.appliedAppList.value = bundle.getStringArrayList("checked")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = true,
                isWhiteList = false,
                checked = viewModel.appliedAppList.value.toTypedArray()
            )
            navigate(R.id.nav_scope, args.toBundle())
        }

        lifecycleScope.launch {
            viewModel.targetSettingList.collect {
                binding.targetApps.text = String.format(getString(R.string.template_setting_count), it.size)
            }
        }
        lifecycleScope.launch {
            viewModel.appliedAppList.collect {
                binding.appliedApps.text = String.format(getString(R.string.template_applied_count), it.size)
            }
        }

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
