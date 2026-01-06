package icu.nullptr.hidemyapplist.common

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.os.Binder
import android.os.Build
import java.lang.reflect.Field
import java.util.Random
import java.util.zip.ZipFile

object Utils {

    fun generateRandomString(length: Int): String {
        val leftLimit = 97   // letter 'a'
        val rightLimit = 122 // letter 'z'
        val random = Random()
        val buffer = StringBuilder(length)
        for (i in 0 until length) {
            val randomLimitedInt = leftLimit + (random.nextFloat() * (rightLimit - leftLimit + 1)).toInt()
            buffer.append(randomLimitedInt.toChar())
        }
        return buffer.toString()
    }

    fun <T> binderLocalScope(block: () -> T): T {
        val identity = Binder.clearCallingIdentity()
        val result = block()
        Binder.restoreCallingIdentity(identity)
        return result
    }

    fun getInstalledPackagesCompat(pms: IPackageManager, flags: Long, userId: Int): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getInstalledPackages(flags, userId)
        } else {
            pms.getInstalledPackages(flags.toInt(), userId)
        }.list
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

    fun generateRandomHex(length: Int): String {
        val allowedChars = ('a'..'f') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
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

    fun isSamsung(): Boolean {
        try {
            val semPlatformIntField: Field =
                Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            semPlatformIntField.isAccessible = true
            return semPlatformIntField.getInt(null) >= 0
        } catch (_: Throwable) {
            return false
        }
    }
}
