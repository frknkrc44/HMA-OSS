package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.AppPresets
import icu.nullptr.hidemyapplist.common.Utils

class RootAppsPreset(private val appPresets: AppPresets) : BasePreset(NAME) {
    companion object {
        const val NAME = "root_apps"
        const val ACCESS_SUPERUSER_PERM = "\u0000a\u0000n\u0000d\u0000r\u0000o\u0000i\u0000d\u0000.\u0000p\u0000e\u0000r\u0000m\u0000i\u0000s\u0000s\u0000i\u0000o\u0000n\u0000.\u0000A\u0000C\u0000C\u0000E\u0000S\u0000S\u0000_\u0000S\u0000U\u0000P\u0000E\u0000R\u0000U\u0000S\u0000E\u0000R"
        const val MOZILLA_WHITELIST = "\u0000o\u0000r\u0000g\u0000.\u0000m\u0000o\u0000z\u0000i\u0000l\u0000l\u0000a\u0000.\u0000g\u0000e\u0000c\u0000k\u0000o"
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
        "com.softwarebakery.drivedroid",
        "com.tester.wpswpatester",
        "com.paget96.lsandroid",
        "ua.polodarb.gmsflags",
        "com.tortel.syslog",
        "com.jhc.detach",
        "com.sunilpaulmathew.debloater",
        "com.rk.taskmanager",

        // NetHunter related apps
        "com.mayank.rucky",
        "org.csploit.android",
        "whid.usb.injector",
        "de.tu_darmstadt.seemoo.nexmon",
        "remote.hid.keyboard.client",
        "de.srlabs.snoopsnitch",
        "com.hijacker",
        "su.sniff.cepter",

        // Hookers
        "me.jsonet.jshook",
        "com.simo.fhook",

        // Scene's "Core Edition" cannot be detected in the Xposed preset
        "com.omarea.vtools",

        // some equalizer apps are using the root required driver packs
        "james.dsp",

        // kernel managers
        "flar2.exkernelmanager",
        "com.franco.kernel",
        "com.lybxlpsv.kernelmanager",
        "com.html6405.boefflakernelconfig",
        "ccc71.st.cpu",
        "com.umang96.radon",
        "com.rve.rvkernelmanager",
        "id.xms.xtrakernelmanager",
    )

    val libNames = arrayOf(
        "libkernelsu.so",
        "libapd.so",
        "libmagisk.so",
        "libmagiskboot.so",
        "libmmrl-file-manager.so",
        "libmmrl-kernelsu.so",
        "libzakoboot.so",
    )

    val assetNames = arrayOf(
        "gamma_profiles.json",
        "main.jar",
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // Some of detectors trying to abuse the ACCESS_SUPERUSER permission
        if (appPresets.getPresetByName(DetectorAppsPreset.NAME)?.containsPackage(packageName) ?: false) {
            return false
        }

        // All uFirewall apps
        if (packageName.startsWith("dev.ukanth.ufirewall")) {
            return true
        }

        // All Viper4Android/ViperFX apps
        if (Utils.endsWithMultiple(packageName, ".viper4android", ".viperfx")) {
            return true
        }

        // All libxzr apps (konabess, hkf, ...)
        if (Utils.startsWithMultiple(packageName, "xzr.", "moe.xzr.")) {
            return true
        }

        // LSPosed and LSPatch
        if (packageName.startsWith("org.lsposed")) {
            return true
        }

        // All Busybox related apps
        if (packageName.contains(".busybox")) {
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

        return checkSplitPackages(appInfo) { key, zipFile ->
            val manifestStr = appPresets.readManifest(key, zipFile)

            // Whitelist the Mozilla apps (why a browser app has ACCESS_SUPERUSER?)
            if (manifestStr.contains(MOZILLA_WHITELIST)) {
                return@checkSplitPackages false
            }

            // Many older rooted apps had this permission for an old Superuser implementation
            // It is not used anymore, but can be good to use it as rooted app indicator
            // Thanks to @F640 for giving this idea
            if (manifestStr.contains(ACCESS_SUPERUSER_PERM)) {
                return@checkSplitPackages true
            }

            if (findAppsFromLibs(zipFile, libNames) || findAppsFromAssets(zipFile, assetNames)) {
                return@checkSplitPackages true
            }

            return@checkSplitPackages false
        }
    }
}
