package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils

class CustomROMPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "custom_rom"
    }

    override val exactPackageNames = setOf(
        "io.chaldeaprjkt.gamespace",
        "powersaver.pro",
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // LineageOS overlays
        if (Utils.containsMultiple(appInfo.sourceDir, "_lineage", "lineage_")) {
            return true
        }

        // LineageOS apps
        if (Utils.startsWithMultiple(packageName, "lineageos.", "org.lineageos.")) {
            return true
        }

        // CAF (CodeAuroraForum) apps
        if (packageName.startsWith("com.caf.")) {
            return true
        }

        // CalyxOS
        if (packageName.startsWith("org.calyxos.")) {
            return true
        }

        // AOSPA
        if (packageName.startsWith("co.aospa.")) {
            return true
        }

        // OmniROM
        if (packageName.startsWith("org.omnirom.")) {
            return true
        }

        // ProtonAOSP
        if (packageName.startsWith("org.protonaosp.")) {
            return true
        }

        // EvoX (just added by the community request)
        if (Utils.startsWithMultiple(packageName, "org.evolution.", "org.evolutionx.") ||
            Utils.endsWithMultiple(packageName, ".evolution", ".evolutionx")) {
            return true
        }

        // Several AOSP ROMs
        if (Utils.startsWithMultiple(
                packageName,
                "com.android.system.switch.",
                "com.accents.",
                "com.alpha.",
                "com.android.systemui.",
                "com.android.theme.",
                "com.bootleggers.",
                "com.custom.overlay.",
                "com.gnonymous.gvisualmod.",
                "com.libremobileos.",
                "com.nikgapps.",
                "com.potato.",
        )) {
            return true
        }

        if (Utils.endsWithMultiple(
                packageName,
                ".overlay.fog",
        )) {
            return true
        }

        // Xiaomi.EU apps
        if (packageName.startsWith("eu.xiaomi.")) {
            return true
        }

        // TODO: Add more custom ROM apps
        return false
    }
}
