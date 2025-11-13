package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.Utils
import java.util.zip.ZipFile

class SDhizukuAppsPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "shizuku_dhizuku"
        const val DHIZUKU_PROVIDER = "\u0000c\u0000o\u0000m\u0000.\u0000r\u0000o\u0000s\u0000a\u0000n\u0000.\u0000d\u0000h\u0000i\u0000z\u0000u\u0000k\u0000u\u0000.\u0000s\u0000e\u0000r\u0000v\u0000e\u0000r\u0000.\u0000p\u0000r\u0000o\u0000v\u0000i\u0000d\u0000e\u0000r"
        const val SHIZUKU_PROVIDER = "\u0000r\u0000i\u0000k\u0000k\u0000a\u0000.\u0000s\u0000h\u0000i\u0000z\u0000u\u0000k\u0000u\u0000.\u0000S\u0000h\u0000i\u0000z\u0000u\u0000k\u0000u\u0000P\u0000r\u0000o\u0000v\u0000i\u0000d\u0000e\u0000r"
    }

    override val exactPackageNames = setOf(
        "com.rosan.dhizuku"
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // All Shizuku apps
        if (packageName.startsWith("moe.shizuku.")) {
            return true
        }

        ZipFile(appInfo.sourceDir).use { zipFile ->
            val manifestStr = AppPresets.instance.readManifest(packageName, zipFile)

            if (Utils.containsMultiple(manifestStr, SHIZUKU_PROVIDER, DHIZUKU_PROVIDER)) {
                return true
            }
        }

        return false
    }
}