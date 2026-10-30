package icu.nullptr.hidemyapplist.common

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import icu.nullptr.hidemyapplist.common.Utils.isPackageAvailable
import org.frknkrc44.hma_oss.common.BuildConfig

object OSUtils {
    private val PACKAGES_TO_CHECK = listOf(
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.inputmethod.latin",
        "com.google.android.ext.shared",
    )

    fun collectOSInfo(context: Context, serviceVersion: String?, workMode: Int) = buildString {
        val pkgMgr = context.packageManager
        val isHOS = SystemProperties.get("ro.mi.os.version.incremental").isNotBlank()
        val isMIUI = pkgMgr.isPackageAvailable("com.miui.system")

        append("HMA-OSS Log")
        append("\nApp version: ")
        append("${BuildConfig.APP_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})")
        append("\nService version: ")
        append(serviceVersion)
        append("\nWork mode: ")
        append(workMode)
        append("\nFingerprint: ")
        append(Build.FINGERPRINT)
        append("\nAndroid SDK: ")
        append("${Build.VERSION.SDK_INT} (P ${Build.VERSION.PREVIEW_SDK_INT})")
        append("\nKnown codenames: [")
        append(SystemProperties.get("ro.build.version.known_codenames"))
        append("]\nIs MIUI/HyperOS: ")
        append(isHOS || isMIUI)
        append("\nIs oplus: ")
        append(SystemProperties.get("ro.build.version.oplusrom").isNotBlank())
        append("\nIs Samsung: ")
        append(isSamsung())
        append("\nIs Pixel: ")
        append(pkgMgr.isPackageAvailable("com.google.android.apps.customization.pixel"))
        append("\nIs LineageOS: ")
        append(pkgMgr.getPackagesForUid(Constants.UID_SYSTEM)?.any { "lineage" in it })

        PACKAGES_TO_CHECK.forEach {
            append("\nIs $it available: ")
            append(pkgMgr.isPackageAvailable(it))
        }
    }

    fun isSamsung(): Boolean {
        try {
            val semPlatformIntField =
                Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            semPlatformIntField.isAccessible = true
            return semPlatformIntField.getInt(null) >= 0
        } catch (_: Throwable) {
            return false
        }
    }
}
