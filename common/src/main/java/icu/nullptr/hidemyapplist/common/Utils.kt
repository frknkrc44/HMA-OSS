package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.os.Binder
import android.os.Build
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

    fun getInstalledApplicationsCompat(pms: IPackageManager, flags: Long, userId: Int): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getInstalledApplications(flags, userId)
        } else {
            pms.getInstalledApplications(flags.toInt(), userId)
        }.list
    }

    fun getPackageUidCompat(pms: IPackageManager, packageName: String, flags: Long, userId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getPackageUid(packageName, flags, userId)
        } else {
            pms.getPackageUid(packageName, flags.toInt(), userId)
        }
    }

    fun getPackageInfoCompat(pms: IPackageManager, packageName: String, flags: Long, userId: Int): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getPackageInfo(packageName, flags, userId)
        } else {
            pms.getPackageInfo(packageName, flags.toInt(), userId)
        }
    }

    fun startsWithMultiple(source: String, vararg targets: String): Boolean {
        assert(source.isNotEmpty() && targets.isNotEmpty())

        return targets.any { source.startsWith(it) }
    }

    fun endsWithMultiple(source: String, vararg targets: String): Boolean {
        assert(source.isNotEmpty() && targets.isNotEmpty())

        return targets.any { source.endsWith(it) }
    }

    fun containsMultiple(source: String, vararg targets: String): Boolean {
        assert(source.isNotEmpty() && targets.isNotEmpty())

        return targets.any { source.contains(it) }
    }

    fun getPackageNameFromResolveInfo(resolveInfo: ResolveInfo): String {
        return resolveInfo.resolvePackageName ?:
            resolveInfo.activityInfo?.packageName ?:
            resolveInfo.serviceInfo?.packageName ?:
            resolveInfo.providerInfo!!.packageName
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

    inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
        this.filter { (key, value) -> predicate(key, value) }.forEach { this.remove(it.key) }
    }

    inline fun <K, V> MutableMap<K, V>.removeIfWithCount(predicate: (K, V) -> Boolean): Int {
        return this.filter { (key, value) -> predicate(key, value) }.count { this.remove(it.key) != null }
    }

    fun cleanRemnantsFromConfig(config: JsonConfig) {
        // STEP 1: Remove empty app and settings templates
        config.templates.removeIf { _, template -> template.appList.isEmpty() }
        config.settingsTemplates.removeIf { _, template -> template.settingsList.isEmpty() }

        // STEP 2: Remove mismatching items
        for (app in config.scope.values) {
            app.applyTemplates.removeIf { !config.templates.containsKey(it) }
            app.applyPresets.removeIf { it !in AppPresets.instance.presetNames }
            app.applySettingTemplates.removeIf { !config.settingsTemplates.containsKey(it) }
            app.applySettingsPresets.removeIf { it !in SettingsPresets.instance.presetNames }
        }
    }
}
