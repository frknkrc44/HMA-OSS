package icu.nullptr.hidemyapplist.ui.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.PropertyUtils
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.enabledString
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.util.ConfigUtils.Companion.getLocale
import icu.nullptr.hidemyapplist.util.LangList
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import icu.nullptr.hidemyapplist.util.SuUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentSettingsBinding
import org.frknkrc44.hma_oss.ui.activity.BaseActivity
import org.frknkrc44.hma_oss.ui.activity.MainActivity
import org.frknkrc44.hma_oss.ui.preference.AppIconPreference
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val binding by viewBinding<FragmentSettingsBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.toolbar) {
            setupToolbar(
                toolbar = this,
                title = getString(R.string.title_settings),
                navigationIcon = R.drawable.baseline_arrow_back_24,
                navigationOnClick = { navController.navigateUp() }
            )
            // isTitleCentered = true
        }

        runBlocking {
            PrefManager.isLauncherIconInvisible.emit(findEnabledAppComponent(hmaApp) == null)
        }

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
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

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragment = childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, pref.fragment!!)
        fragment.arguments = pref.extras
        childFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    class SettingsPreferenceDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent
                "systemWallpaper" -> PrefManager.systemWallpaper
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                "detailLog" -> ConfigManager.detailLog
                "hideIcon" -> PrefManager.hideIcon
                "bypassRiskyPackageWarning" -> PrefManager.bypassRiskyPackageWarning
                "appDataIsolation" -> ConfigManager.altAppDataIsolation
                "voldAppDataIsolation" -> ConfigManager.altVoldAppDataIsolation
                "skipSystemAppDataIsolation" -> ConfigManager.skipSystemAppDataIsolation
                "disableActivityLaunchProtection" -> ConfigManager.disableActivityLaunchProtection
                "forceMountData" -> ConfigManager.forceMountData
                "packageQueryWorkaround" -> ConfigManager.packageQueryWorkaround
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "language" -> PrefManager.locale
                "themeColor" -> PrefManager.themeColor
                "darkTheme" -> PrefManager.darkTheme.toString()
                "maxLogSize" -> ConfigManager.maxLogSize.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getInt(key: String, defValue: Int): Int {
            return when (key) {
                "systemWallpaperAlpha" -> PrefManager.systemWallpaperAlpha
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                "systemWallpaper" -> PrefManager.systemWallpaper = value
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                "detailLog" -> ConfigManager.detailLog = value
                "forceMountData" -> ConfigManager.forceMountData = value
                "hideIcon" -> PrefManager.hideIcon = value
                "bypassRiskyPackageWarning" -> PrefManager.bypassRiskyPackageWarning = value
                "disableActivityLaunchProtection" -> ConfigManager.disableActivityLaunchProtection = value
                "appDataIsolation" -> ConfigManager.altAppDataIsolation = value
                "voldAppDataIsolation" -> ConfigManager.altVoldAppDataIsolation = value
                "skipSystemAppDataIsolation" -> ConfigManager.skipSystemAppDataIsolation = value
                "packageQueryWorkaround" -> ConfigManager.packageQueryWorkaround = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "language" -> PrefManager.locale = value!!
                "themeColor" -> PrefManager.themeColor = value!!
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                "maxLogSize" -> ConfigManager.maxLogSize = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putInt(key: String, value: Int) {
            when (key) {
                "systemWallpaperAlpha" -> PrefManager.systemWallpaperAlpha = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class DataIsolationPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings_data_isolation, rootKey)

            findPreference<SwitchPreferenceCompat>("appDataIsolation")?.let {
                it.summary = getString(R.string.settings_need_reboot) + "\n\n" +
                        getString(
                            R.string.settings_default_value,
                            PropertyUtils.isAppDataIsolationEnabled.enabledString(resources)
                        )
            }

            findPreference<SwitchPreferenceCompat>("voldAppDataIsolation")?.let {
                it.summary = getString(R.string.settings_need_reboot) + "\n\n" +
                        getString(
                            R.string.settings_default_value,
                            PropertyUtils.isVoldAppDataIsolationEnabled.enabledString(resources)
                        )

                it.setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    if (enabled) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.settings_warning)
                            .setMessage(R.string.settings_vold_warning)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                it.isChecked = true
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                it.isChecked = false
                            }
                            .setCancelable(false)
                            .show()
                    }
                    !enabled
                }
            }
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        private fun configureDataIsolation() {
            findPreference<Preference>("dataIsolation")?.let {
                it.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                it.summary = when {
                    it.isEnabled -> getString(
                        R.string.settings_data_isolation_summary,
                        if (ConfigManager.altAppDataIsolation) getString(R.string.settings_overwritten)
                        else PropertyUtils.isAppDataIsolationEnabled.enabledString(resources),
                        if (ConfigManager.altVoldAppDataIsolation) getString(R.string.settings_overwritten)
                        else PropertyUtils.isVoldAppDataIsolationEnabled.enabledString(resources),
                        ConfigManager.forceMountData.enabledString(resources)
                    )
                    else -> getString(R.string.settings_data_isolation_unsupported)
                }
            }
        }

        @Suppress("deprecation")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<ListPreference>("language")?.let {
                val userLocale = getLocale()
                val entries = buildList {
                    for (lang in LangList.LOCALES) {
                        if (lang == "SYSTEM") add(getString(R.string.follow_system))
                        else {
                            val locale = Locale.forLanguageTag(lang)
                            add(locale.getDisplayName(locale))
                        }
                    }
                }
                it.entries = entries.toTypedArray()
                it.entryValues = LangList.LOCALES
                if (it.value == "SYSTEM") {
                    it.summary = getString(R.string.follow_system)
                } else {
                    val locale = Locale.forLanguageTag(it.value)
                    it.summary = if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(userLocale) else locale.getDisplayName(userLocale)
                }
                it.setOnPreferenceChangeListener { _, newValue ->
                    val locale = getLocale()
                    val config = resources.configuration
                    config.setLocale(locale)
                    hmaApp.resources.updateConfiguration(config, resources.displayMetrics)
                    activity?.recreate()
                    true
                }
            }

            findPreference<Preference>("translation")?.let {
                it.summary = getString(R.string.settings_translate_summary, getString(R.string.app_name))
                it.setOnPreferenceClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Constants.TRANSLATE_URL.toUri()))
                    true
                }
            }

            findPreference<SwitchPreferenceCompat>("followSystemAccent")?.also {
                it.isVisible = DynamicColors.isDynamicColorAvailable()

                it.setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }
            }

            findPreference<ListPreference>("themeColor")?.also {
                if (!DynamicColors.isDynamicColorAvailable()) it.dependency = null

                it.setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }
            }

            findPreference<ListPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
                val newMode = (newValue as String).toInt()
                if (PrefManager.darkTheme != newMode) {
                    AppCompatDelegate.setDefaultNightMode(newMode)
                    activity?.recreate()
                }
                true
            }

            findPreference<SwitchPreferenceCompat>("systemWallpaper")?.apply {
                isEnabled = findPreference<SwitchPreferenceCompat>("blackDarkTheme")?.isChecked != true
                setOnPreferenceChangeListener { _, value ->
                    val activity = requireActivity() as MainActivity
                    activity.readyToKill = false

                    if (value as Boolean) {
                        activity.finish()
                        startActivity(Intent(activity, activity.javaClass))
                    } else {
                        activity.recreate()
                    }

                    true
                }
            }

            findPreference<SeekBarPreference>("systemWallpaperAlpha")?.apply {
                setOnPreferenceChangeListener { _, value ->
                    (requireActivity() as BaseActivity).applyWallpaperBackgroundColor(value as Int)

                    true
                }
            }

            lifecycleScope.launch {
                PrefManager.isLauncherIconInvisible
                    .flowWithLifecycle(lifecycle)
                    .collect { _ ->
                        findPreference<AppIconPreference>("launcherIcon")?.apply {
                            updateHolder()
                        }
                    }
            }

            findPreference<Preference>("clearUninstalledPackageConfigs")?.apply {
                setOnPreferenceClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_clear_uninstalled_app_configs)
                        .setMessage(R.string.settings_no_undone_warning)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val progressDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.settings_clear_uninstalled_app_configs)
                                .setView(R.layout.dialog_loading)
                                .setCancelable(false)
                                .create()

                            progressDialog.show()

                            ConfigManager.clearUninstalledAppConfigs { isSuccess ->
                                lifecycleScope.launch {
                                    progressDialog.dismiss()

                                    if (isSuccess) {
                                        showToast(android.R.string.ok)
                                    }
                                }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .setCancelable(false)
                        .show()

                    true
                }
            }

            findPreference<SwitchPreferenceCompat>("blackDarkTheme")?.apply {
                isEnabled = findPreference<SwitchPreferenceCompat>("systemWallpaper")?.isChecked != true
                setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }
            }

            configureDataIsolation()

            findPreference<Preference>("stopSystemService")?.setOnPreferenceClickListener {
                if (ServiceClient.serviceVersion != 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_is_clean_env)
                        .setMessage(R.string.settings_is_clean_env_summary)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            ServiceClient.stopService(true)
                            showToast(R.string.settings_stop_system_service)
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            ServiceClient.stopService(false)
                            showToast(R.string.settings_stop_system_service)
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                } else showToast(R.string.home_xposed_service_off)
                true
            }

            findPreference<Preference>("forceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_is_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result = SuUtils.execPrivileged("rm -rf /data/misc/hide_my_applist*")
                        if (result) showToast(R.string.settings_force_clean_env_toast_successful)
                        else showToast(R.string.settings_permission_denied)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            configureDataIsolation()
        }
    }
}
