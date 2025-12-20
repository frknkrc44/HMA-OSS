package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo

class SuspiciousAppsPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "sus_apps"
    }

    override val exactPackageNames = setOf(
        "com.reveny.vbmetafix.service",
        "berserker.android.apps.sshdroid",
        "com.iamaner.oneclickfreeze",
        "com.shamanland.privatescreenshots",

        // Remote desktop apps
        "com.devolutions.remotedesktopmanager",
        "com.anydesk.anydeskandroid",
        "com.carriez.flutter_hbb",

        // Suspicious app markets
        "com.happymod.apk",
        "com.pd.pdhelper",
        "cm.aptoide.pt",

        // File managers
        "com.speedsoftware.rootexplorer",
        "me.zhanghai.android.files",
        "com.lonelycatgames.Xplore",
        "org.fossify.filemanager",
        "com.amaze.filemanager",

        // Gravitybox Unlocker
        "com.ceco.gravitybox.unlocker",
    )

    /*
    val libNames = arrayOf<String>(
        // Note that LSPlant is used in some of games too
        // I will not add it as suspicious app indicator anymore
        //
        // "liblsplant.so",

        // TODO: Add more suspicious apps by checking for libs
    )
     */

    val assetNames = arrayOf(
        // ~All possible APK editors
        "APKEditor.pk8",
        "testkey.pk8",
        "key/testkey.pk8",

        // TODO: Add more suspicious apps by checking for files
    )

    override fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName

        // All NetHunter apps
        if (packageName.startsWith("com.offsec.")) {
            return true
        }

        // Termux, all of its plugins and some of Termux forks
        if (packageName.startsWith("com.termux")) {
            return true
        }

        // All RealVNC apps (categorized as suspicious, because some of apps checking for them)
        if (packageName.startsWith("com.realvnc.")) {
            return true
        }

        // FX File Manager
        if (packageName.startsWith("nextapp.fx")) {
            return true
        }

        // TotalCommander and its plugins
        if (packageName.startsWith("com.ghisler.")) {
            return true
        }

        // ZDevs apps (ZArchiver etc.)
        if (packageName.startsWith("ru.zdevs.")) {
            return true
        }

        // MiXplorer, MiXplorer Silver and its plugins
        if (packageName.startsWith("com.mixplorer")) {
            return true
        }

        // MT Manager
        if (packageName.startsWith("bin.mt.plus")) {
            return true
        }

        // All StrAI apps
        if (packageName.startsWith("com.x0.strai.")) {
            return true
        }

        // All Microsoft remote control apps
        if (packageName.startsWith("com.microsoft.rdc.")) {
            return true
        }

        // All TeamViewer apps
        if (packageName.startsWith("com.teamviewer.")) {
            return true
        }

        return checkSplitPackages(appInfo) { _, zipFile ->
            if (/*findAppsFromLibs(zipFile, libNames) ||*/ findAppsFromAssets(zipFile, assetNames)) {
                return@checkSplitPackages true
            }

            // TODO: Add more suspicious apps
            return@checkSplitPackages false
        }
    }
}
