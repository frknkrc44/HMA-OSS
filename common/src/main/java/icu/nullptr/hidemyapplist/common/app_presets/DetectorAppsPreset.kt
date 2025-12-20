package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo

class DetectorAppsPreset  : BasePreset(NAME) {
    companion object {
        const val NAME = "detector_apps"
    }

    override val exactPackageNames = setOf(
        // Detector apps
        "com.reveny.nativecheck",
        "icu.nullptr.nativetest",
        "io.github.rabehx.securify",
        "com.zhenxi.hunter",
        "io.github.vvb2060.mahoshojo",
        "io.github.huskydg.memorydetector",
        "org.akanework.checker",
        "icu.nullptr.applistdetector",
        "com.byxiaorun.detector",
        "com.kimchangyoun.rootbeerFresh.sample",
        "com.androidfung.drminfo",
        "com.kikyps.crackme",
        "org.matrix.demo",
        "com.rem01gaming.disclosure",
        "luna.safe.luna",
        "com.AndroLua",
        "com.detect.mt",
        "io.liankong.riskdetector",
        "com.suisho.rc",
        "com.ahmed.security_tester",
        "id.my.pjm.qbcd_okr_dvii",
        "wu.Zygisk.Detector",
        "com.atominvention.rootchecker",
        "com.joeykrim.rootcheck",
        "com.studio.duckdetector",

        // EnvChecksDemo (thanks @gavdoc38)
        "com.dexprotector.detector.envchecks",

        // Play Integrity checkers
        "krypton.tbsafetychecker",
        "gr.nikolasspyr.integritycheck",
        "com.henrikherzig.playintegritychecker",
        "com.thend.integritychecker",
        "com.flinkapps.safteynet",

        // Other checkers
        "com.bryancandi.knoxcheck",
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // All Garfield packages
        if (packageName.startsWith("me.garfieldhan.")) {
            return true
        }

        // Key attestation apps
        if (packageName.endsWith(".keyattestation")) {
            return true
        }

        return false
    }
}
