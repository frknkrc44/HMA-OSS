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
        "dev.ukanth.ufirewall.donate",
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
        "com.pittvandewitt.viperfx",
        "com.aam.viper4android",
        "james.dsp",

        // Scene's "Core Edition" cannot be detected in the Xposed preset
        "com.omarea.vtools",

        // kernel managers
        "flar2.exkernelmanager",
        "com.franco.kernel",
        "com.lybxlpsv.kernelmanager",
        "com.html6405.boefflakernelconfig",
        "ccc71.st.cpu",
        "com.umang96.radon",

        // NetHunter related apps
        "com.mayank.rucky",
        "org.csploit.android",
        "whid.usb.injector",
        "de.tu_darmstadt.seemoo.nexmon",
        "remote.hid.keyboard.client",
        "com.softwarebakery.drivedroid",
        "de.srlabs.snoopsnitch",
        "com.hijacker",
        "su.sniff.cepter",

        // WPS WPA Tester
        "com.tester.wpswpatester",

        // LSpeed
        "com.paget96.lsandroid",

        // GMS Flags
        "ua.polodarb.gmsflags",

        // SysLog
        "com.tortel.syslog",

        // Stericson Busybox installer
        "stericson.busybox",
        "stericson.busybox.donate",

        // Integrity Box
        "meow.helper",
    )

    val libNames = arrayOf(
        "libkernelsu.so",
        "libksud.so",
        "libksu_susfs.so",
        "libapd.so",
        "libmagisk.so",
        "libmagiskboot.so",
        "libmmrl-kernelsu.so",
        "libzakoboot.so",
        "libzakosign.so",
    )

    val assetNames = arrayOf(
        "gamma_profiles.json",
        "main.jar",
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // All NetHunter apps (Nethunter app, NH KeX, ...)
        if (Utils.startsWith(packageName, "com.offsec.nethunter.")) {
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
            val manifestStr = AppPresets.instance.readManifest(packageName, zipFile)

            if (findAppsFromLibs(zipFile, libNames) || findAppsFromAssets(zipFile, assetNames)) {
                return true
            }

            // Many older root apps add this permission
            if (Utils.containsMultiple(manifestStr, ACCESS_SUPERUSER)) {
                return true
            }
        }

        return false
    }
}
