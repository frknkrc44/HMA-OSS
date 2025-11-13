package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils
import java.util.zip.ZipFile

class RootAppsPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "root_apps"
    }

    override val exactPackageNames = setOf(
        // rooted apps
        "io.github.a13e300.ksuwebui",
        "com.fox2code.mmm",
        "id.kuato.diskhealth",
        "com.sunilpaulmathew.debloater",
        "com.garyodernichts.downgrader",
        "eu.roggstar.getmitokens",
        "io.github.domi04151309.powerapp",
        "eu.roggstar.luigithehunter.batterycalibrate",
        "at.or.at.plugoffairplane",
        "tk.giesecke.phoenix",
        "com.corphish.nightlight.generic",
        "com.zinaro.cachecleanerwidget",
        "de.buttercookie.simbadroid",
        "simple.reboot.com",
        "ru.evgeniy.dpitunnel",
        "ca.mudar.fairphone.peaceofmind",
        "com.gitlab.giwiniswut.rwremount",
        "com.machiav3lli.backup",
        "com.bartixxx.opflashcontrol",
        "dev.ukanth.ufirewall",
        "org.nuntius35.wrongpinshutdown",
        "ru.nsu.bobrofon.easysshfs",
        "x1125io.initdlight",
        "com.byyoung.setting",
        "web1n.stopapp",
        "org.adaway",
        "com.mrsep.ttlchanger",
        "mattecarra.accapp",
        "io.github.saeeddev94.pixelnr",
        "com.js.nowakelock",
        "me.twrp.twrpapp",
        "com.slash.batterychargelimit",
        "com.valhalla.thor",
        "me.itejo443.bindhosts",

        // Scene's "Core Edition" cannot be detected in the Xposed preset
        "com.omarea.vtools",

        // kernel managers
        "flar2.exkernelmanager",
        "com.franco.kernel",
        "com.lybxlpsv.kernelmanager",
        "com.html6405.boefflakernelconfig",
        "ccc71.st.cpu",
    )

    val libNames = arrayOf(
        "libkernelsu.so",
        "libapd.so",
        "libmagisk.so",
        "libmagiskboot.so",
        "libmmrl-kernelsu.so",
        "libzakoboot.so",
    )

    val assetNames = arrayOf(
        "gamma_profiles.json",
        "main.jar",
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // All libxzr apps (konabess, hkf, ...)
        if (Utils.startsWithMultiple(packageName, "xzr.", "moe.xzr.")) {
            return true
        }

        // LSPosed and LSPatch
        if (packageName.startsWith("org.lsposed")) {
            return true
        }

        // Iconify
        if (packageName.startsWith("com.drdisagree.iconify")) {
            return true
        }

        // MMRL
        if (packageName.startsWith("com.dergoogler.mmrl")) {
            return true
        }

        // Magisk
        if (packageName.endsWith(".magisk")) {
            return true
        }

        // APatch
        if (packageName.contains(".apatch.") || packageName.endsWith(".apatch")) {
            return true
        }

        // DataBackup
        if (packageName.startsWith("com.xayah.databackup")) {
            return true
        }

        // SmartPack Kernel Manager + Busybox Installer
        if (packageName.startsWith("com.smartpack.")) {
            return true
        }

        // F-Droid Privileged
        if (packageName.startsWith("org.fdroid.fdroid.privileged")) {
            return true
        }

        ZipFile(appInfo.sourceDir).use { zipFile ->
            if (findAppsFromLibs(zipFile, libNames) || findAppsFromAssets(zipFile, assetNames)) {
                return true
            }
        }

        return false
    }
}
