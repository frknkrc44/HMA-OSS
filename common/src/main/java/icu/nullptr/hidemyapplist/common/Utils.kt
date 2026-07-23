package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Binder
import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.removeIf
import java.util.zip.ZipFile

object Utils {

    fun generateRandomString(length: Int, allowedChars: List<Char>): String {
        return (0 until length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun generateRandomHex(length: Int) = generateRandomString(
        length,
        ('a' .. 'f') + ('0' .. '9'),
    )

    fun <T> binderLocalScope(block: () -> T): T {
        val identity = Binder.clearCallingIdentity()
        val result = block()
        Binder.restoreCallingIdentity(identity)
        return result
    }

    fun IPackageManager.getInstalledApplicationsCompat(flags: Long, userId: Int): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getInstalledApplications(flags, userId)
        } else {
            this.getInstalledApplications(flags.toInt(), userId)
        }.list
    }

    fun IPackageManager.getPackageUidCompat(packageName: String, flags: Long, userId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getPackageUid(packageName, flags, userId)
        } else {
            this.getPackageUid(packageName, flags.toInt(), userId)
        }
    }

    fun IPackageManager.getPackageInfoCompat(packageName: String, flags: Long, userId: Int): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getPackageInfo(packageName, flags, userId)
        } else {
            this.getPackageInfo(packageName, flags.toInt(), userId)
        }
    }

    fun String.startsWithMultiple(vararg targets: String): Boolean {
        assert(isNotEmpty() && targets.isNotEmpty())

        return targets.any { startsWith(it) }
    }

    fun String.endsWithMultiple(vararg targets: String): Boolean {
        assert(isNotEmpty() && targets.isNotEmpty())

        return targets.any { endsWith(it) }
    }

    fun String.containsMultiple(vararg targets: String): Boolean {
        assert(isNotEmpty() && targets.isNotEmpty())

        return targets.any { contains(it) }
    }

    fun ResolveInfo.getPackageName(): String {
        return resolvePackageName ?:
            activityInfo?.packageName ?:
            serviceInfo?.packageName ?:
            providerInfo!!.packageName
    }

    fun checkSplitPackages(appInfo: ApplicationInfo, onZipFile: (String, ZipFile) -> Boolean): Boolean {
        val allLocations = setOf(appInfo.sourceDir, appInfo.publicSourceDir) /*+
                (appInfo.splitSourceDirs ?: arrayOf()) +
                (appInfo.splitPublicSourceDirs ?: arrayOf())*/

        return allLocations.any { filePath ->
            ZipFile(filePath).use { zipFile ->
                if (onZipFile(filePath, zipFile)) {
                    return true
                }
            }

            return false
        }
    }

    fun JsonConfig.cleanRemnantsFromConfig() {
        // STEP 1: Remove empty app and settings templates
        templates.removeIf { _, template -> template.appList.isEmpty() }
        settingsTemplates.removeIf { _, template -> template.settingsList.isEmpty() }

        // STEP 2: Remove mismatching items
        for (app in scope.values) {
            app.applyTemplates.removeIf { !templates.containsKey(it) }
            app.applyPresets.removeIf { it !in AppPresets.instance.presetNames }
            app.applySettingTemplates.removeIf { !settingsTemplates.containsKey(it) }
            app.applySettingsPresets.removeIf { it !in SettingsPresets.instance.presetNames }
        }
    }

    fun getUserFromCallingUid(uid: Int) = uid / 100000

    fun IPackageManager.isAppInstalled(packageName: String, userId: Int = 0) =
        getPackageUidCompat(packageName, 0, userId) >= 0

    fun PackageManager.isAppInstalled(packageName: String) = try {
        getPackageUid(packageName, 0) >= 0
    } catch (_: Throwable) {
        false
    }

    fun ApplicationInfo.isSystemApp() = flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

    val conflictedModules = arrayOf("com.tsng.hidemyapplist", "com.google.android.hmal")
}
