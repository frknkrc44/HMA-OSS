package org.frknkrc44.hma_oss.ui.fragment

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.androidbroadcast.vbpd.viewBinding
import icu.nullptr.hidemyapplist.common.Constants.CONFIG_VERSION_NO_SETTINGS
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.Utils.cleanRemnantsFromConfig
import icu.nullptr.hidemyapplist.common.Utils.removeIf
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.util.contentResolver
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import icu.nullptr.hidemyapplist.util.PackageHelper.loadAppLabel
import kotlinx.coroutines.launch
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentBackupRestoreBinding
import org.frknkrc44.hma_oss.databinding.LayoutListEmptyBinding
import java.util.Date
import java.util.Locale
import kotlin.getValue

class BackupRestoreFragment : Fragment(R.layout.fragment_backup_restore) {

    private val binding by viewBinding(FragmentBackupRestoreBinding::bind)

    private val args by lazy { navArgs<BackupRestoreFragmentArgs>() }

    private lateinit var importedConfig: JsonConfig

    private val isBackupMode by lazy { args.value.isBackupMode }
    private val includeSettings get() = binding.switchSettings.isChecked
    private val trimConfig get() = binding.switchTrimConfig.isChecked
    private val overwriteApps get() = binding.switchOverwriteApps.isChecked
    private val overwriteTemplates get() = binding.switchOverwriteTemplates.isChecked
    private val overwriteSettingsTemplates get() = binding.switchOverwriteTemplates.isChecked

    private enum class BRCategory {
        APP,
        TEMPLATE,
        SETTINGS_TEMPLATE,
    }

    private val markedForBackup = BRCategory.entries.associateWith { mutableSetOf<String>() }

    private val backupSAFLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) backup@{ uri ->
            if (uri == null) return@backup
            val output = contentResolver.openOutputStream(uri)

            if (output == null) showToast(R.string.home_export_failed)
            else {
                clearNotImportedItems {
                    if (!includeSettings) {
                        val newConfig = JsonConfig(CONFIG_VERSION_NO_SETTINGS)
                        newConfig.templates.putAll(importedConfig.templates)
                        newConfig.settingsTemplates.putAll(importedConfig.settingsTemplates)
                        newConfig.scope.putAll(importedConfig.scope)
                        importedConfig = newConfig
                    }

                    showToast(R.string.home_exported)
                    output.write(importedConfig.toString().toByteArray())
                    output.close()

                    navController.navigateUp()
                }
            }
        }

    private val restoreSAFLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) restore@{ uri ->
            if (uri == null) {
                navController.navigateUp()
                return@restore
            }

            runCatching {
                val backupContent = contentResolver
                    .openInputStream(uri)!!.reader().use { it.readText() }
                importedConfig = JsonConfig.parse(backupContent)
                loadScreenContents()
            }.onFailure {
                it.printStackTrace()
                navController.navigateUp()
                MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(false)
                    .setTitle(R.string.home_import_failed)
                    .setMessage(it.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.show_crash_log) { _, _ ->
                        MaterialAlertDialogBuilder(requireActivity())
                            .setCancelable(false)
                            .setTitle(R.string.home_import_failed)
                            .setMessage(it.stackTraceToString())
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    .show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.isVisible = false

        setupToolbar(
            binding.toolbar,
            title = getString(R.string.home_backup_and_restore),
            subtitle = getString(
                if (isBackupMode) R.string.home_backup_config
                        else R.string.home_restore_config
            ),
            menuRes = R.menu.menu_backup_restore,
            onMenuOptionSelected = this@BackupRestoreFragment::onMenuOptionSelected
        )

        if (isBackupMode) {
            importedConfig = ConfigManager.getRawConfig(true)
            loadScreenContents()
        } else {
            restoreSAFLauncher.launch("application/json")
        }

        setEdge2EdgeFlags(binding.root)
    }

    @SuppressLint("DefaultLocale")
    @Suppress("deprecation")
    private fun reloadScreenContents() {
        binding.manageApps.subText = getString(
            R.string.backup_restore_items_count,
            markedForBackup[BRCategory.APP]!!.size,
        )
        binding.templateList.subText = getString(
            R.string.backup_restore_items_count,
            markedForBackup[BRCategory.TEMPLATE]!!.size,
        )
        binding.settingsTemplateList.subText = getString(
            R.string.backup_restore_items_count,
            markedForBackup[BRCategory.SETTINGS_TEMPLATE]!!.size,
        )
    }

    private fun loadScreenContents() {
        binding.root.isVisible = true

        markedForBackup[BRCategory.APP]!!.addAll(importedConfig.scope.keys)
        markedForBackup[BRCategory.TEMPLATE]!!.addAll(importedConfig.templates.keys)
        markedForBackup[BRCategory.SETTINGS_TEMPLATE]!!.addAll(importedConfig.settingsTemplates.keys)

        with(binding.manageApps) {
            text = getString(R.string.backup_restore_apps)
            setOnClickListener {
                showDialogToSelect(BRCategory.APP)
            }
        }

        with(binding.templateList) {
            text = getString(R.string.backup_restore_templates)
            setOnClickListener {
                showDialogToSelect(BRCategory.TEMPLATE)
            }
        }

        with(binding.settingsTemplateList) {
            text = getString(R.string.backup_restore_settings_templates)
            setOnClickListener {
                showDialogToSelect(BRCategory.SETTINGS_TEMPLATE)
            }
        }

        with(binding.switchSettings) {
            isChecked = importedConfig.configVersion != CONFIG_VERSION_NO_SETTINGS
            isEnabled = isChecked
        }

        with(binding.switchTrimConfig) {
            isVisible = isBackupMode
        }

        with(binding.switchOverwriteApps) {
            isVisible = !isBackupMode
            isChecked = true

            setOnCheckedChangeListener { _, value ->
                setText(
                    if (value) {
                        R.string.settings_overwrite
                    } else {
                        R.string.backup_restore_append
                    }
                )
            }
        }

        with(binding.switchOverwriteTemplates) {
            isVisible = !isBackupMode
            isChecked = true

            setOnCheckedChangeListener { _, value ->
                setText(
                    if (value) {
                        R.string.settings_overwrite
                    } else {
                        R.string.backup_restore_append
                    }
                )
            }
        }

        with(binding.switchOverwriteSettingsTemplates) {
            isVisible = !isBackupMode
            isChecked = true

            setOnCheckedChangeListener { _, value ->
                setText(
                    if (value) {
                        R.string.settings_overwrite
                    } else {
                        R.string.backup_restore_append
                    }
                )
            }
        }

        reloadScreenContents()
    }

    private fun showDialogToSelect(category: BRCategory) {
        val items = when (category) {
            BRCategory.APP -> importedConfig.scope.keys
            BRCategory.TEMPLATE -> importedConfig.templates.keys
            BRCategory.SETTINGS_TEMPLATE -> importedConfig.settingsTemplates.keys
        }

        val labels = when (category) {
            BRCategory.APP -> items.map { loadAppLabel(it) }
            BRCategory.TEMPLATE, BRCategory.SETTINGS_TEMPLATE -> items
        }.toTypedArray()

        val checked = items.map {
            markedForBackup[category]!!.contains(it)
        }.toBooleanArray()

        val title = when (category) {
            BRCategory.APP -> getString(R.string.title_app_manage)
            BRCategory.TEMPLATE, BRCategory.SETTINGS_TEMPLATE -> getString(R.string.title_template_manage)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val category = markedForBackup[category]
                category!!.clear()
                category.addAll(items.mapIndexedNotNull { i, name ->
                    if (checked[i]) name else null
                })

                reloadScreenContents()
            }

        if (items.isNotEmpty()) {
            dialog.setMultiChoiceItems(labels, checked) { _, i, value ->
                checked[i] = value
            }
        } else {
            val emptyView = LayoutListEmptyBinding.inflate(layoutInflater)
            emptyView.root.isVisible = true
            emptyView.listEmptyIcon.setImageResource(R.drawable.sentiment_very_dissatisfied_24px)
            emptyView.listEmptyText.isVisible = false
            dialog.setView(emptyView.root)
        }

        dialog.show()
    }

    private fun onRestore() = clearNotImportedItems {
        if (!overwriteApps || !overwriteTemplates) {
            val config = ConfigManager.getRawConfig(false)

            if (!overwriteApps) {
                config.scope.map {
                    importedConfig.scope.putIfAbsent(it.key, it.value)
                }
            }

            if (!overwriteTemplates) {
                config.templates.map {
                    importedConfig.templates.putIfAbsent(it.key, it.value)
                }
            }

            if (!overwriteSettingsTemplates) {
                config.settingsTemplates.map {
                    importedConfig.settingsTemplates.putIfAbsent(it.key, it.value)
                }
            }
        }

        if (!includeSettings || importedConfig.configVersion == CONFIG_VERSION_NO_SETTINGS) {
            val currentConfig = ConfigManager.getRawConfig(true)

            with(currentConfig.scope) {
                clear()
                putAll(importedConfig.scope)
            }

            with(currentConfig.templates) {
                clear()
                putAll(importedConfig.templates)
            }

            with(currentConfig.settingsTemplates) {
                clear()
                putAll(importedConfig.settingsTemplates)
            }

            ConfigManager.importConfig(currentConfig.toString())
        } else {
            ConfigManager.importConfig(importedConfig.toString())
        }

        showToast(android.R.string.ok)
        navController.navigateUp()
    }

    private fun clearNotImportedItems(onFinish: () -> Unit) {
        importedConfig.scope.removeIf { pkg, _ ->
            !markedForBackup[BRCategory.APP]!!.contains(pkg)
        }

        importedConfig.templates.removeIf { template, _ ->
            !markedForBackup[BRCategory.TEMPLATE]!!.contains(template)
        }

        importedConfig.settingsTemplates.removeIf { template, _ ->
            !markedForBackup[BRCategory.SETTINGS_TEMPLATE]!!.contains(template)
        }

        if (isBackupMode) {
            cleanRemnantsFromConfig(importedConfig)

            if (trimConfig) {
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_clear_uninstalled_app_configs)
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create()

                ConfigManager.clearUninstalledAppConfigs(importedConfig) {
                    lifecycleScope.launch {
                        progressDialog.dismiss()

                        onFinish()
                    }
                }
            } else {
                onFinish()
            }
        } else {
            onFinish()
        }
    }

    @Suppress("unused")
    private fun onMenuOptionSelected(item: MenuItem) {
        if (isBackupMode) {
            val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.getDefault()).format(Date())
            backupSAFLauncher.launch("HMA-OSS_config_$date.json")
        } else {
            onRestore()
        }
    }
}
