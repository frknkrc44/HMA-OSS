package icu.nullptr.hidemyapplist.common

import org.frknkrc44.hma_oss.common.BuildConfig

object Constants {
    const val PROVIDER_AUTHORITY = "${BuildConfig.APP_PACKAGE_NAME}.ServiceProvider"
    const val GMS_PACKAGE_NAME = "com.google.android.gms"
    const val GSF_PACKAGE_NAME = "com.google.android.gsf"
    const val VENDING_PACKAGE_NAME = "com.android.vending"
    const val TRANSLATE_URL = "https://crowdin.com/project/frknkrc44-hma-oss"

    const val ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.zygote.app_data_isolation"
    const val ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.sys.vold_app_data_isolation_enabled"

    const val UID_SYSTEM = 1000

    val packagesShouldNotHide = setOf(
        "android",
        "android.media",
        "android.uid.system",
        "android.uid.shell",
        "android.uid.systemui",
        "com.android.permissioncontroller",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.settings",
        "com.google.android.webview",
        "com.google.android.providers.media.module"
    )
}
