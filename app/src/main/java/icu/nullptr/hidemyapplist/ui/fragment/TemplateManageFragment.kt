package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.TemplateAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentTemplateManageBinding
import org.frknkrc44.hma_oss.ui.fragment.SettingsTemplateConfFragmentArgs
import org.frknkrc44.hma_oss.ui.util.toTargetSettingList

class TemplateManageFragment : Fragment(R.layout.fragment_template_manage) {

    private val binding by viewBinding<FragmentTemplateManageBinding>()
    private val adapter = TemplateAdapter(this::navigateToSettings)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_template_manage),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() },
            menuRes = R.menu.menu_template_manage,
            onMenuOptionSelected = {
                when (it.itemId) {
                    R.id.menu_info -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.title_template_manage)
                            .setMessage(R.string.template_usage_hint)
                            .setNegativeButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        )

        binding.newBlacklistTemplate.setOnClickListener {
            navigateToSettings(ConfigManager.TemplateInfo(null, ConfigManager.PTType.APP, false))
        }
        binding.newWhitelistTemplate.setOnClickListener {
            navigateToSettings(ConfigManager.TemplateInfo(null, ConfigManager.PTType.APP, true))
        }
        binding.newSettingTemplate.setOnClickListener {
            navigateToSettings(ConfigManager.TemplateInfo(null, ConfigManager.PTType.SETTINGS, false))
        }
        binding.templateList.layoutManager = LinearLayoutManager(context)
        binding.templateList.adapter = adapter

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

    private fun navigateToSettings(info: ConfigManager.TemplateInfo) {
        when (info.type) {
            ConfigManager.PTType.APP -> navigateToAppTemplateSettings(info)
            ConfigManager.PTType.SETTINGS -> navigateToSettingTemplateSettings(info)
        }
    }

    private fun navigateToAppTemplateSettings(info: ConfigManager.TemplateInfo) {
        setFragmentResultListener("template_settings") { _, bundle ->
            fun deal() {
                var name = bundle.getString("name")
                val appliedList = bundle.getStringArrayList("appliedList")!!
                val targetList = bundle.getStringArrayList("targetList")!!
                if (info.name == null) { // New template
                    if (name.isNullOrEmpty()) return
                    ConfigManager.updateTemplate(name, JsonConfig.Template(info.isWhiteList, targetList.toSet()))
                    ConfigManager.updateTemplateAppliedApps(name, appliedList)
                } else {                 // Existing template
                    if (name == null) {
                        ConfigManager.deleteTemplate(info.name)
                    } else {
                        if (name.isEmpty()) name = info.name
                        if (name != info.name) ConfigManager.renameTemplate(info.name, name)
                        ConfigManager.updateTemplate(name, JsonConfig.Template(info.isWhiteList, targetList.toSet()))
                        ConfigManager.updateTemplateAppliedApps(name, appliedList)
                    }
                }
            }
            deal()
            adapter.updateList()
            clearFragmentResultListener("template_settings")
        }

        val args = TemplateSettingsFragmentArgs(info.name, info.isWhiteList)
        navigate(R.id.nav_template_settings, args.toBundle())
    }

    private fun navigateToSettingTemplateSettings(info: ConfigManager.TemplateInfo) {
        setFragmentResultListener("settings_template_conf") { _, bundle ->
            fun deal() {
                var name = bundle.getString("name")
                val appliedList = bundle.getStringArrayList("appliedList")!!
                val targetList = bundle.getBundle("settingList")!!.toTargetSettingList()
                if (info.name == null) { // New template
                    if (name.isNullOrEmpty()) return
                    ConfigManager.updateSettingTemplate(
                        name,
                        JsonConfig.SettingsTemplate(targetList.toSet())
                    )
                    ConfigManager.updateSettingTemplateAppliedApps(name, appliedList)
                } else {                 // Existing template
                    if (name == null) {
                        ConfigManager.deleteSettingTemplate(info.name)
                    } else {
                        if (name.isEmpty()) name = info.name
                        if (name != info.name) ConfigManager.renameSettingTemplate(info.name, name)
                        ConfigManager.updateSettingTemplate(
                            name,
                            JsonConfig.SettingsTemplate(targetList.toSet())
                        )
                        ConfigManager.updateSettingTemplateAppliedApps(name, appliedList)
                    }
                }
            }
            deal()
            adapter.updateList()
            clearFragmentResultListener("settings_template_conf")
        }

        val args = SettingsTemplateConfFragmentArgs(info.name)
        navigate(R.id.nav_setting_template_conf, args.toBundle())
    }
}
