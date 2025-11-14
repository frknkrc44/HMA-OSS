package icu.nullptr.hidemyapplist.common

import org.frknkrc44.hma_oss.common.BuildConfig

object Constants {
    const val PROVIDER_AUTHORITY = "${BuildConfig.APP_PACKAGE_NAME}.ServiceProvider"
    const val GMS_PACKAGE_NAME = "com.google.android.gms"
    const val GSF_PACKAGE_NAME = "com.google.android.gsf"
    const val VENDING_PACKAGE_NAME = "com.android.vending"
    const val TRANSLATE_URL = "https://crowdin.com/project/frknkrc44-hma-oss"

    const val UID_SYSTEM = 1000

    val gmsPackages = arrayOf(GMS_PACKAGE_NAME, GSF_PACKAGE_NAME)
    val riskyPackages = arrayOf(VENDING_PACKAGE_NAME) + gmsPackages

    const val SETTINGS_GLOBAL = "global"
    const val SETTINGS_SYSTEM = "system"
    const val SETTINGS_SECURE = "secure"

    const val SETTINGS_TYPE_GLOBAL: Int = 0
    const val SETTINGS_TYPE_SYSTEM: Int = 1
    const val SETTINGS_TYPE_SECURE: Int = 2
    // const val SETTINGS_TYPE_SSAID: Int = 3
    // const val SETTINGS_TYPE_CONFIG: Int = 4

    const val FAKE_INSTALLATION_SOURCE_DISABLED = 0
    const val FAKE_INSTALLATION_SOURCE_USER = 1
    const val FAKE_INSTALLATION_SOURCE_SYSTEM = 2

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
