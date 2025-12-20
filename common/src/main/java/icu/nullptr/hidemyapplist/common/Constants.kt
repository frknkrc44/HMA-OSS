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

    const val FAKE_INSTALLATION_SOURCE_DISABLED = 0
    const val FAKE_INSTALLATION_SOURCE_USER = 1
    const val FAKE_INSTALLATION_SOURCE_SYSTEM = 2

    /**
     * Defines the GID for the group that allows write access to the internal media storage.
     */
    const val SDCARD_RW_GID: Int = 1015

    /**
     * Defines the GID for the group that allows write access to the internal media storage.
     */
    const val MEDIA_RW_GID: Int = 1023

    /**
     * Access to installed package details
     */
    const val PACKAGE_INFO_GID: Int = 1032

    /**
     * GID that gives access to USB OTG (unreliable) volumes on /mnt/media_rw/<vol name>
    </vol> */
    const val EXTERNAL_STORAGE_GID: Int = 1077

    /**
     * GID that gives write access to app-private data directories on external
     * storage (used on devices without sdcardfs only).
     */
    const val EXT_DATA_RW_GID: Int = 1078

    /**
     * GID that gives write access to app-private OBB directories on external
     * storage (used on devices without sdcardfs only).
     */
    const val EXT_OBB_RW_GID: Int = 1079

    /**
     * GID that corresponds to the INTERNET permission.
     * Must match the value of AID_INET.
     */
    const val INET_GID: Int = 3003

    /**
     * Defines the gid shared by all applications running under the same profile.
     */
    const val SHARED_USER_GID: Int = 9997

    val GID_PAIRS = mapOf(
        "SDCARD_RW_GID" to SDCARD_RW_GID,
        "MEDIA_RW_GID" to MEDIA_RW_GID,
        "PACKAGE_INFO_GID" to PACKAGE_INFO_GID,
        "EXTERNAL_STORAGE_GID" to EXTERNAL_STORAGE_GID,
        "EXT_DATA_RW_GID" to EXT_DATA_RW_GID,
        "EXT_OBB_RW_GID" to EXT_OBB_RW_GID,
        "INET_GID" to INET_GID,
        "SHARED_USER_GID" to SHARED_USER_GID,
    )

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
