package icu.nullptr.hidemyapplist.common

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import org.frknkrc44.hma_oss.common.BuildConfig
import java.lang.reflect.Field

object OSUtils {
    private val PACKAGES_TO_CHECK = listOf(
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.inputmethod.latin",
        "com.google.android.ext.shared",
    )

    fun collectOSInfo(context: Context, serviceVersion: String?) = buildString {
        append("HMA-OSS Log")
        append("\nApp version: ")
        append("${BuildConfig.APP_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})")
        append("\nService version: ")
        append(serviceVersion)
        append("\nFingerprint: ")
        append(Build.FINGERPRINT)
        append("\nAndroid SDK: ")
        append("${Build.VERSION.SDK_INT} (P ${Build.VERSION.PREVIEW_SDK_INT})")
        append("\nKnown codenames: [")
        append(SystemProperties.get("ro.build.version.known_codenames"))
        append("]\nIs HyperOS: ")
        append(SystemProperties.get("ro.mi.os.version.incremental").isNotBlank())
        append("\nIs MIUI: ")
        append(isPackageExists(context, "com.miui.system"))
        append("\nIs oplus: ")
        append(SystemProperties.get("ro.build.version.oplusrom").isNotBlank())
        append("\nIs Samsung: ")
        append(isSamsung())
        append("\nIs Pixel: ")
        append(isPackageExists(context, "com.google.android.apps.customization.pixel"))

        PACKAGES_TO_CHECK.forEach {
            append("\nIs $it available: ")
            append(isPackageExists(context, it))
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

    fun isPackageExists(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageUid(packageName, 0) > 0
        } catch (_: Throwable) {
            false
        }
    }
}