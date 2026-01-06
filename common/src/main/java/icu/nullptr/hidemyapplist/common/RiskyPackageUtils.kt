package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import icu.nullptr.hidemyapplist.common.Utils.checkSplitPackages

object RiskyPackageUtils {
    private const val GMS_PROP = "\u0000c\u0000o\u0000m\u0000.\u0000g\u0000o\u0000o\u0000g\u0000l\u0000e\u0000.\u0000a\u0000n\u0000d\u0000r\u0000o\u0000i\u0000d\u0000.\u0000g\u0000m\u0000s\u0000."
    private const val FIREBASE_PROP = "\u0000c\u0000o\u0000m\u0000.\u0000g\u0000o\u0000o\u0000g\u0000l\u0000e\u0000.\u0000f\u0000i\u0000r\u0000e\u0000b\u0000a\u0000s\u0000e\u0000."

    internal val ignoredForRiskyPackagesList = mutableSetOf<String>()

    fun appHasGMSConnection(query: String) = query in ignoredForRiskyPackagesList

    internal fun tryToAddIntoGMSConnectionList(appInfo: ApplicationInfo, packageName: String, loggerFunction: ((String) -> Unit)?): Boolean {
        if (packageName in ignoredForRiskyPackagesList) return false

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
}
