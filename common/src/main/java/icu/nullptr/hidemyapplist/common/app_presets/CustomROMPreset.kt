package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils.containsMultiple
import icu.nullptr.hidemyapplist.common.Utils.endsWithMultiple
import icu.nullptr.hidemyapplist.common.Utils.startsWithMultiple

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
        if (appInfo.sourceDir.containsMultiple("_lineage", "lineage_")) {
            return true
        }

        // LineageOS apps
        if (packageName.startsWithMultiple("lineageos.", "org.lineageos.")) {
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
        if (packageName.startsWithMultiple("org.evolution.", "org.evolutionx.") ||
            packageName.endsWithMultiple( ".evolution", ".evolutionx")) {
            return true
        }

        // Several AOSP ROMs
        if (packageName.startsWithMultiple(
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

        if (packageName.endsWith(".overlay.fog")) {
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
