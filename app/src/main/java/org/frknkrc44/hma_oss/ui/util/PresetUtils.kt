package org.frknkrc44.hma_oss.ui.util

import icu.nullptr.hidemyapplist.common.app_presets.AccessibilityAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.CustomROMPreset
import icu.nullptr.hidemyapplist.common.app_presets.DetectorAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.RootAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.SDhizukuAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.SuspiciousAppsPreset
import icu.nullptr.hidemyapplist.common.app_presets.XposedModulesPreset
import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset
import icu.nullptr.hidemyapplist.common.settings_presets.DeveloperOptionsPreset
import icu.nullptr.hidemyapplist.common.settings_presets.InputMethodPreset
import org.frknkrc44.hma_oss.R

object PresetUtils {
    val presetMap = mapOf(
        AccessibilityAppsPreset.NAME to R.string.preset_accessibility_apps,
        CustomROMPreset.NAME to R.string.preset_custom_rom,
        DetectorAppsPreset.NAME to R.string.preset_detector_apps,
        RootAppsPreset.NAME to R.string.preset_root_apps,
        SDhizukuAppsPreset.NAME to R.string.preset_shizuku_dhizuku,
        SuspiciousAppsPreset.NAME to R.string.preset_sus_apps,
        XposedModulesPreset.NAME to R.string.preset_xposed,
    )

    val settingsPresetMap = mapOf(
        AccessibilityPreset.NAME to R.string.settings_preset_accessibility,
        DeveloperOptionsPreset.NAME to R.string.settings_preset_dev_options,
        InputMethodPreset.NAME to R.string.settings_preset_input_method,
    )
}
