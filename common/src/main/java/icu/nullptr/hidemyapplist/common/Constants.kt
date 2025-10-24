package icu.nullptr.hidemyapplist.common

import org.frknkrc44.hma_oss.common.BuildConfig

object Constants {
    const val PROVIDER_AUTHORITY = "${BuildConfig.APP_PACKAGE_NAME}.ServiceProvider"
    private const val GMS_PACKAGE_NAME = "com.google.android.gms"
    private const val GSF_PACKAGE_NAME = "com.google.android.gsf"
    const val VENDING_PACKAGE_NAME = "com.android.vending"
    const val TRANSLATE_URL = "https://crowdin.com/project/frknkrc44-hma-oss"

    const val UID_SYSTEM = 1000

    val gmsPackages = arrayOf(GMS_PACKAGE_NAME, GSF_PACKAGE_NAME)
    val riskyPackages = arrayOf(VENDING_PACKAGE_NAME) + gmsPackages

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
