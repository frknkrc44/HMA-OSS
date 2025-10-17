package icu.nullptr.hidemyapplist.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.enabledString
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSettingsViewModel
import icu.nullptr.hidemyapplist.util.PackageHelper
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentSettingsBinding

class AppSettingsFragment : Fragment(R.layout.fragment_settings) {
    companion object {
        private const val TAG = "AppSettingsFragment"
    }

    private val binding by viewBinding<FragmentSettingsBinding>()
    private val viewModel by viewModels<AppSettingsViewModel>() {
        val args by navArgs<AppSettingsFragmentArgs>()
        val cfg = ConfigManager.getAppConfig(args.packageName)
        val pack = if (cfg != null) AppSettingsViewModel.Pack(args.packageName, true, cfg)
        else AppSettingsViewModel.Pack(args.packageName, false, JsonConfig.AppConfig())
        AppSettingsViewModel.Factory(pack)
    }

    private fun saveConfig() {
        if (!viewModel.pack.enabled) ConfigManager.setAppConfig(viewModel.pack.app, null)
        else ConfigManager.setAppConfig(viewModel.pack.app, viewModel.pack.config)
    }

    private fun onBack() {
        saveConfig()
        navController.navigateUp()
    }

    override fun onPause() {
        super.onPause()
        saveConfig()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveConfig()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_settings),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() }
        )

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, AppPreferenceFragment())
                .commit()
        }

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

    class AppPreferenceDataStore(private val pack: AppSettingsViewModel.Pack) : PreferenceDataStore() {

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "enableHide" -> pack.enabled
                "useWhiteList" -> pack.config.useWhitelist
                "excludeSystemApps" -> pack.config.excludeSystemApps
                "hideInstallationSource" -> pack.config.hideInstallationSource
                "hideSystemInstallationSource" -> pack.config.hideSystemInstallationSource
                "excludeTargetInstallationSource" -> pack.config.excludeTargetInstallationSource
                "invertActivityLaunchProtection" -> pack.config.invertActivityLaunchProtection
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "enableHide" -> pack.enabled = value
                "useWhiteList" -> pack.config.useWhitelist = value
                "excludeSystemApps" -> pack.config.excludeSystemApps = value
                "hideInstallationSource" -> pack.config.hideInstallationSource = value
                "hideSystemInstallationSource" -> pack.config.hideSystemInstallationSource = value
                "excludeTargetInstallationSource" -> pack.config.excludeTargetInstallationSource = value
                "invertActivityLaunchProtection" -> pack.config.invertActivityLaunchProtection = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class AppPreferenceFragment : PreferenceFragmentCompat() {

        private val parent get() = requireParentFragment() as AppSettingsFragment
        private val pack get() = parent.viewModel.pack

        private fun updateApplyTemplates() {
            findPreference<Preference>("applyTemplates")?.title =
                getString(R.string.app_template_using, pack.config.applyTemplates.size)
        }

        private fun updateApplyPresets() {
            findPreference<Preference>("applyPresets")?.title =
                getString(R.string.app_preset_using, pack.config.applyPresets.size)
        }

        private fun updateApplySettingsPresets() {
            findPreference<Preference>("applySettingsPresets")?.title =
                getString(R.string.app_settings_preset_using, pack.config.applySettingsPresets.size)
        }

        private fun updateExtraAppList(useWhiteList: Boolean) {
            findPreference<Preference>("extraAppList")?.title =
                if (useWhiteList) getString(R.string.app_extra_apps_visible_count, pack.config.extraAppList.size)
                else getString(R.string.app_extra_apps_invisible_count, pack.config.extraAppList.size)
        }

        private fun launchMainActivity(packageName: String) {
            try {
                val pkgMgr = requireContext().packageManager
                val pkgInfo = pkgMgr.getPackageInfo(packageName, 0)
                if (pkgInfo.applicationInfo?.enabled == true) {
                    val resolvedIntent = pkgMgr.getLaunchIntentForPackage(packageName)
                    if (resolvedIntent != null) {
                        startActivity(resolvedIntent)
                    }
                } else {
                    throw RuntimeException("Package is disabled")
                }
            } catch (e: Throwable) {
                showToast(R.string.app_launch_failed)
                ServiceClient.log(Log.ERROR, TAG, e.stackTraceToString())
            }
        }

        @SuppressLint("DiscouragedApi")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = AppPreferenceDataStore(pack)
            setPreferencesFromResource(R.xml.app_settings, rootKey)
            findPreference<Preference>("appInfo")?.let {
                it.icon = PackageHelper.loadAppIcon(pack.app).toDrawable(resources)
                it.title = PackageHelper.loadAppLabel(pack.app)
                it.summary = pack.app
                it.setOnPreferenceClickListener { pref ->
                    MaterialAlertDialogBuilder(pref.context).apply {
                        setTitle(it.title)
                        setItems(
                            R.array.app_action_texts,
                        ) { dialog, which ->
                            when (which) {
                                0 -> {
                                    ServiceClient.forceStop(pack.app, 0)
                                    launchMainActivity(pack.app)
                                }
                                1 -> {
                                    launchMainActivity(pack.app)
                                }
                            }
                        }
                    }.show()

                    true
                }
            }
            findPreference<SwitchPreferenceCompat>("hideInstallationSource")?.setOnPreferenceChangeListener { _, newValue ->
                Toast.makeText(requireContext(),
                    R.string.app_force_stop_warning, Toast.LENGTH_LONG).show()
                true
            }
            findPreference<SwitchPreferenceCompat>("hideSystemInstallationSource")?.setOnPreferenceChangeListener { _, newValue ->
                Toast.makeText(requireContext(),
                    R.string.app_force_stop_warning, Toast.LENGTH_LONG).show()
                true
            }
            findPreference<SwitchPreferenceCompat>("excludeTargetInstallationSource")?.setOnPreferenceChangeListener { _, newValue ->
                Toast.makeText(requireContext(),
                    R.string.app_force_stop_warning, Toast.LENGTH_LONG).show()
                true
            }
            findPreference<SwitchPreferenceCompat>("invertActivityLaunchProtection")?.let {
                it.summary = getString(R.string.app_invert_activity_launch_protection_desc) + "\n\n" +
                        getString(R.string.app_global_activity_launch_protection_state,
                            (!ConfigManager.disableActivityLaunchProtection).enabledString(resources)
                        )
            }
            findPreference<SwitchPreferenceCompat>("useWhiteList")?.setOnPreferenceChangeListener { _, newValue ->
                pack.config.applyTemplates.clear()
                pack.config.extraAppList.clear()
                updateApplyTemplates()
                updateApplyPresets()
                updateExtraAppList(newValue as Boolean)
                true
            }
            findPreference<Preference>("applyTemplates")?.setOnPreferenceClickListener {
                val templates = ConfigManager.getTemplateList().mapNotNull {
                    if (it.isWhiteList == pack.config.useWhitelist) it.name else null
                }.toTypedArray()
                val checked = templates.map {
                    pack.config.applyTemplates.contains(it)
                }.toBooleanArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_template)
                    .setMultiChoiceItems(templates, checked) { _, i, value -> checked[i] = value }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        pack.config.applyTemplates = templates.mapIndexedNotNullTo(mutableSetOf()) { i, name ->
                            if (checked[i]) name else null
                        }
                        updateApplyTemplates()
                    }
                    .show()
                true
            }
            findPreference<Preference>("applyPresets")?.setOnPreferenceClickListener {
                val presetNames = AppPresets.instance.getAllPresetNames()
                val presetTranslations = presetNames.map { name ->
                    try {
                        val id = resources.getIdentifier(
                            "preset_${name}",
                            "string",
                            BuildConfig.APPLICATION_ID
                        )

                        return@map if (id != 0) { getString(id) } else { name }
                    } catch (_: Throwable) {}

                    name
                }

                val presets = presetNames.zip(presetTranslations).toMap().toSortedMap()
                val checked = presets.keys.map {
                    pack.config.applyPresets.contains(it)
                }.toBooleanArray()
                val presetValues = presets.values.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_preset)
                    .setMultiChoiceItems(presetValues, checked) { _, i, value -> checked[i] = value }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        pack.config.applyPresets = presetValues.mapIndexedNotNullTo(mutableSetOf()) { i, name ->
                            if (checked[i]) presets.filterValues { v -> v == name }.keys.first() else null
                        }
                        updateApplyPresets()
                    }
                    .show()
                true
            }
            findPreference<Preference>("applySettingsPresets")?.setOnPreferenceClickListener {
                val presetNames = SettingsPresets.instance.getAllPresetNames()
                val presetTranslations = presetNames.map { name ->
                    try {
                        val id = resources.getIdentifier(
                            "settings_preset_${name}",
                            "string",
                            BuildConfig.APPLICATION_ID
                        )

                        return@map if (id != 0) { getString(id) } else { name }
                    } catch (_: Throwable) {}

                    name
                }

                val presets = presetNames.zip(presetTranslations).toMap().toSortedMap()
                val checked = presets.keys.map {
                    pack.config.applySettingsPresets.contains(it)
                }.toBooleanArray()
                val presetValues = presets.values.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_preset)
                    .setMultiChoiceItems(presetValues, checked) { _, i, value -> checked[i] = value }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        pack.config.applySettingsPresets = presetValues.mapIndexedNotNullTo(mutableSetOf()) { i, name ->
                            if (checked[i]) presets.filterValues { v -> v == name }.keys.first() else null
                        }
                        updateApplySettingsPresets()
                    }
                    .show()
                true
            }
            findPreference<Preference>("extraAppList")?.setOnPreferenceClickListener {
                parent.setFragmentResultListener("app_select") { _, bundle ->
                    pack.config.extraAppList = bundle.getStringArrayList("checked")!!.toMutableSet()
                    updateExtraAppList(pack.config.useWhitelist)
                    parent.clearFragmentResultListener("app_select")
                }

                val args = ScopeFragmentArgs(
                    filterOnlyEnabled = false,
                    checked = pack.config.extraAppList.toTypedArray()
                )
                navigate(R.id.nav_scope, args.toBundle())
                true
            }
            updateApplyTemplates()
            updateApplyPresets()
            updateApplySettingsPresets()
            updateExtraAppList(pack.config.useWhitelist)
        }
    }
}
