package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import java.util.zip.ZipFile

class XposedModulesPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "xposed"
    }

    override val exactPackageNames = setOf<String>()

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        ZipFile(appInfo.sourceDir).use { zipFile ->
            // Legacy Xposed method
            if (zipFile.getEntry("assets/xposed_init") != null) {
                return true
            }

            // New LSPosed method
            if (zipFile.getEntry("META-INF/xposed/module.prop") != null) {
                return true
            }
        }

        return false
    }
}
