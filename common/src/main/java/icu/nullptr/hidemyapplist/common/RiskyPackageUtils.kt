package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils.checkSplitPackages

object RiskyPackageUtils {
    private const val GMS_PROP = "\u0000c\u0000o\u0000m\u0000.\u0000g\u0000o\u0000o\u0000g\u0000l\u0000e\u0000.\u0000a\u0000n\u0000d\u0000r\u0000o\u0000i\u0000d\u0000.\u0000g\u0000m\u0000s\u0000."
    private const val FIREBASE_PROP = "\u0000c\u0000o\u0000m\u0000.\u0000g\u0000o\u0000o\u0000g\u0000l\u0000e\u0000.\u0000f\u0000i\u0000r\u0000e\u0000b\u0000a\u0000s\u0000e\u0000."

    private val ignoredForRiskyPackagesList = mutableSetOf<String>()

    // Add apps in that list when they have connections but not specified in the manifest file
    private val explicitlyIgnoredPackages = arrayOf(
        "com.anydesk.anydeskandroid",
    )

    fun appHasGMSConnection(query: String) = query in ignoredForRiskyPackagesList || query in explicitlyIgnoredPackages

    internal fun tryToAddIntoGMSConnectionList(appInfo: ApplicationInfo, packageName: String, loggerFunction: ((String) -> Unit)?): Boolean {
        if (appHasGMSConnection(packageName)) return false

        return checkSplitPackages(appInfo) { key, zipFile ->
            val manifestStr = AppPresets.instance.readManifest(key, zipFile)

            // Checking with binary because the Android system sucks
            if (manifestStr.contains(GMS_PROP) || manifestStr.contains(FIREBASE_PROP)) {
                if (ignoredForRiskyPackagesList.add(packageName)) {
                    loggerFunction?.invoke("@appHasGMSConnection $packageName added in ignored packages list")
                    return@checkSplitPackages true
                }
            }

            return@checkSplitPackages false
        }
    }

    internal fun removeAppFromList(packageName: String) = ignoredForRiskyPackagesList.remove(packageName)

    internal fun clearAppList() = ignoredForRiskyPackagesList.clear()
}
