package org.frknkrc44.hma_oss.ui.fragment

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import icu.nullptr.hidemyapplist.data.fetchLatestUpdate
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.attrDrawable
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.getColor
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.homeItemBackgroundColor
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import icu.nullptr.hidemyapplist.ui.util.contentResolver
import icu.nullptr.hidemyapplist.ui.util.navigate
import icu.nullptr.hidemyapplist.ui.util.setEdge2EdgeFlags
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentHomeBinding
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding<FragmentHomeBinding>()

    private val backupSAFLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) backup@{ uri ->
            if (uri == null) return@backup
            ConfigManager.configFile.inputStream().use { input ->
                contentResolver.openOutputStream(uri).use { output ->
                    if (output == null) showToast(R.string.home_export_failed)
                    else input.copyTo(output)
                }
            }
            showToast(R.string.home_exported)
        }

    private val restoreSAFLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) restore@{ uri ->
            if (uri == null) return@restore
            runCatching {
                val backup = contentResolver
                    .openInputStream(uri)?.reader().use { it?.readText() }
                    ?: throw IOException(getString(R.string.home_import_file_damaged))
                ConfigManager.importConfig(backup)
                showToast(R.string.home_import_successful)
            }.onFailure {
                it.printStackTrace()
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
        with(binding.toolbar) {
            setupToolbar(
                toolbar = binding.toolbar,
                title = getString(R.string.app_name),
            )
            // isTitleCentered = true
        }

        setEdge2EdgeFlags(binding.root)
    }

    override fun onStart() {
        super.onStart()

        val serviceVersion = ServiceClient.serviceVersion
        var color = when {
            serviceVersion == 0 -> getColor(R.color.invalid)
            else -> themeColor(android.R.attr.colorPrimary)
        }

        if (PrefManager.systemWallpaper) color -= 0x55000000

        with(binding.statusCard) {
            root.setCardBackgroundColor(color)
            root.outlineAmbientShadowColor = color
            root.outlineSpotShadowColor = color

            if (serviceVersion > 0) {
                moduleStatusIcon.setImageResource(R.drawable.sentiment_calm_24px)
                val versionNameSimple = BuildConfig.VERSION_NAME.substringBefore(".r")
                moduleStatus.text =
                    getString(R.string.home_xposed_activated, versionNameSimple)
                root.setOnLongClickListener {
                    ConfigManager.saveConfig()
                    showToast(android.R.string.ok)

                    true
                }
            } else {
                moduleStatusIcon.setImageResource(R.drawable.sentiment_very_dissatisfied_24px)
                moduleStatus.setText(R.string.home_xposed_not_activated)
            }

            if (serviceVersion != 0) {
                if (serviceVersion < org.frknkrc44.hma_oss.common.BuildConfig.SERVICE_VERSION) {
                    serviceStatus.text =
                        getString(R.string.home_xposed_service_old)
                } else {
                    serviceStatus.text =
                        getString(R.string.home_xposed_service_on, serviceVersion)
                }
                filterCount.visibility = View.VISIBLE
                filterCount.text =
                    getString(R.string.home_xposed_filter_count, ServiceClient.filterCount)
            } else {
                serviceStatus.setText(R.string.home_xposed_service_off)
                filterCount.visibility = View.GONE
            }
        }

        with(binding.howToUse.root.parent as ViewGroup) {
            val childCount = childCount

            val softCorner: Float = resources.displayMetrics.density * 24
            val squareCorner: Float = resources.displayMetrics.density * 8
            val pad = (resources.displayMetrics.density * 16).toInt()

            for (i in 0..< childCount) {
                getChildAt(i).apply {
                    (this as ViewGroup).apply {
                        val textColor = themeColor(
                            com.google.android.material.R.attr.colorOnSurface,
                        )

                        findViewById<TextView>(android.R.id.text1).setTextColor(textColor)
                        findViewById<ImageView>(android.R.id.icon).setColorFilter(textColor)
                    }

                    (layoutParams as LinearLayout.LayoutParams).apply {
                        setMargins(pad, 0, pad, 0)
                    }

                    val backgroundDrawable = GradientDrawable()
                    backgroundDrawable.setColor(homeItemBackgroundColor())

                    if (i == 0) {
                        backgroundDrawable.setCornerRadii(
                            floatArrayOf(
                                softCorner,
                                softCorner,
                                softCorner,
                                softCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner
                            )
                        )
                    } else if (i == childCount - 1) {
                        backgroundDrawable.setCornerRadii(
                            floatArrayOf(
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                softCorner,
                                softCorner,
                                softCorner,
                                softCorner
                            )
                        )
                    } else {
                        backgroundDrawable.setCornerRadii(
                            floatArrayOf(
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner,
                                squareCorner
                            )
                        )
                    }

                    val ripple = attrDrawable(android.R.attr.selectableItemBackground)
                    val layerDrawable = LayerDrawable(arrayOf(
                        backgroundDrawable,
                        ripple,
                    ))

                    background = layerDrawable
                    clipToOutline = true
                }

            }
        }

        with(binding.howToUse) {
            text1.text = getString(R.string.about_how_to_use_title)
            icon.setImageResource(R.drawable.baseline_help_outline_24)
            root.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.about_how_to_use_title)
                    .setMessage(
                        getString(R.string.about_how_to_use_description_1) +
                                "\n\n" +
                                getString(R.string.about_how_to_use_description_2))
                    .setNegativeButton(android.R.string.ok, null)
                    .show()
            }
        }

        with(binding.manageApps) {
            text1.text = getString(R.string.title_app_manage)
            icon.setImageResource(R.drawable.outline_android_24)
            root.setOnClickListener {
                navigate(R.id.nav_app_manage)
            }
        }

        with(binding.manageTemplates) {
            text1.text = getString(R.string.title_template_manage)
            icon.setImageResource(R.drawable.ic_outline_layers_24)
            root.setOnClickListener {
                navigate(R.id.nav_template_manage)
            }
        }

        with(binding.managePresets) {
            text1.text = getString(R.string.title_preset_manage)
            icon.setImageResource(R.drawable.baseline_my_location_24)
            root.setOnClickListener {
                navigate(R.id.nav_presets)
            }
        }

        with(binding.navBulkConfigWizard) {
            text1.text = getString(R.string.title_bulk_config_wizard)
            icon.setImageResource(R.drawable.outline_storage_24)
            root.setOnClickListener {
                navigate(R.id.nav_bulk_config_wizard)
            }
        }

        with(binding.navLogs) {
            text1.text = getString(R.string.title_logs)
            icon.setImageResource(R.drawable.outline_assignment_24)
            root.setOnClickListener {
                navigate(R.id.nav_logs)
            }
        }

        with(binding.navStats) {
            text1.text = getString(R.string.title_filter_stats)
            icon.setImageResource(R.drawable.outline_cleaning_services_24)
            root.setOnClickListener {
                navigate(R.id.nav_stats)
            }
        }

        with(binding.navSettings) {
            text1.text = getString(R.string.title_settings)
            icon.setImageResource(R.drawable.outline_settings_24)
            root.setOnClickListener {
                navigate(R.id.nav_settings)
            }
        }

        with(binding.navAbout) {
            text1.text = getString(R.string.title_about)
            icon.setImageResource(R.drawable.outline_info_24)
            root.setOnClickListener {
                navigate(R.id.nav_about)
            }
        }

        with(binding.backupConfig) {
            if (PrefManager.systemWallpaper) background.alpha = 0xAA

            setOnClickListener {
                val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.getDefault()).format(Date())
                backupSAFLauncher.launch("HMA-OSS_config_$date.json")
            }
        }

        with(binding.restoreConfig) {
            if (PrefManager.systemWallpaper) background.alpha = 0xAA

            setOnClickListener {
                restoreSAFLauncher.launch("application/json")
            }
        }

        lifecycleScope.launch {
            loadUpdateDialog()
        }
    }

    private fun loadUpdateDialog() {
        if (PrefManager.disableUpdate || BuildConfig.VERSION_NAME.count { it == '-' } != 1) return
        fetchLatestUpdate { updateInfo ->
            if (updateInfo.versionName != BuildConfig.VERSION_NAME) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setCancelable(false)
                        .setTitle(getString(R.string.home_new_update, updateInfo.versionName))
                        .setMessage(updateInfo.content)
                        .setPositiveButton("GitHub") { _, _ ->
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    updateInfo.downloadUrl.toUri()
                                )
                            )
                        }
                        .setNegativeButton("Telegram") { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/aerathfuns".toUri()))
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
