package icu.nullptr.hidemyapplist.common

import android.os.SystemProperties

object PropertyUtils {
    private const val ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.zygote.app_data_isolation"
    private const val ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.sys.vold_app_data_isolation_enabled"

    @JvmStatic
    val isAppDataIsolationEnabled: Boolean
        get() = SystemProperties.getBoolean(ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY, true)

    @JvmStatic
    val isVoldAppDataIsolationEnabled: Boolean
        get() = SystemProperties.getBoolean(ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY, false)
}
