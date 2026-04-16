package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.Utils.checkSplitPackages

class AccessibilityAppsPreset(private val appPresets: AppPresets) : BasePreset(NAME) {
    companion object {
        const val NAME = "accessibility_apps"
        const val PERM_ACCESSIBILITY = "\u0000a\u0000n\u0000d\u0000r\u0000o\u0000i\u0000d\u0000.\u0000p\u0000e\u0000r\u0000m\u0000i\u0000s\u0000s\u0000i\u0000o\u0000n\u0000.\u0000B\u0000I\u0000N\u0000D\u0000_\u0000A\u0000C\u0000C\u0000E\u0000S\u0000S\u0000I\u0000B\u0000I\u0000L\u0000I\u0000T\u0000Y\u0000_\u0000S\u0000E\u0000R\u0000V\u0000I\u0000C\u0000E"
    }

    override val exactPackageNames = setOf<String>()

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        // skip detector apps
        if (appPresets.getPresetByName(DetectorAppsPreset.NAME)?.containsPackage(appInfo.packageName) ?: false) {
            return false
        }

        return checkSplitPackages(appInfo) { key, zipFile ->
            val manifestStr = appPresets.readManifest(key, zipFile)

            return@checkSplitPackages manifestStr.contains(PERM_ACCESSIBILITY)
        }
    }
}