package icu.nullptr.hidemyapplist.common.app_presets

import android.content.pm.ApplicationInfo
import java.util.zip.ZipFile

abstract class BasePreset(val name: String) {
    protected abstract val exactPackageNames: Set<String>
    protected val packageNames = mutableSetOf<String>()

    protected abstract fun canBeAddedIntoPreset(appInfo: ApplicationInfo): Boolean

    fun containsPackage(packageName: String) = exactPackageNames.contains(packageName) || packageNames.contains(packageName)

    val packages get() = packageNames + exactPackageNames

    fun clearPackageList() = packageNames.clear()

    fun addPackageInfoPreset(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName
        if (!containsPackage(packageName) && canBeAddedIntoPreset(appInfo)) {
            packageNames.add(packageName)
            return true
        }

        return false
    }

    fun removePackageFromPreset(packageName: String): Boolean {
        if (exactPackageNames.contains(packageName)) return false
        return packageNames.remove(packageName)
    }

    protected fun findAppsFromLibs(zipFile: ZipFile, libNames: Array<String>): Boolean {
        val architectures = arrayOf("arm64-v8a", "armeabi-v7a")

        for (entry in libNames) {
            for (arch in architectures) {
                if (zipFile.getEntry("lib/$arch/$entry") != null) {
                    return true
                }
            }
        }

        return false
    }

    protected fun findAppsFromAssets(zipFile: ZipFile, assetNames: Array<String>): Boolean {
        for (entry in assetNames) {
            if (zipFile.getEntry("assets/$entry") != null) {
                return true
            }
        }

        return false
    }

    fun checkSplitPackages(appInfo: ApplicationInfo, onZipFile: (String, ZipFile) -> Boolean): Boolean {
        val allLocations = setOf(appInfo.sourceDir, appInfo.publicSourceDir) +
                (appInfo.splitSourceDirs ?: arrayOf()) +
                (appInfo.splitPublicSourceDirs ?: arrayOf())

        return allLocations.any { filePath ->
            ZipFile(filePath).use { zipFile ->
                if (onZipFile(filePath, zipFile)) {
                    return true
                }
            }

            return false
        }
    }

    override fun toString() = "${javaClass.simpleName} {" +
            " \"exactPackageNames\": $exactPackageNames," +
            " \"packageNames\": $packageNames" +
            " }"
}
