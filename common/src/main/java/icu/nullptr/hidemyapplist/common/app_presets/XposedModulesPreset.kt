package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils.checkSplitPackages

class XposedModulesPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "xposed"
    }

    override val exactPackageNames = setOf<String>()

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        return checkSplitPackages(appInfo) { _, zipFile ->
            // Legacy Xposed method
            if (zipFile.getEntry("assets/xposed_init") != null) {
                return@checkSplitPackages true
            }

            // New LSPosed method
            if (zipFile.getEntry("META-INF/xposed/module.prop") != null) {
                return@checkSplitPackages true
            }

            return@checkSplitPackages false
        }
    }
}
