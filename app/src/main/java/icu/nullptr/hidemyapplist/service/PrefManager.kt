package icu.nullptr.hidemyapplist.service

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import icu.nullptr.hidemyapplist.data.AppConstants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.ui.util.get
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

object PrefManager {

    private const val PREF_LAST_VERSION = "last_version"

    private const val PREF_LOCALE = "language"

    private const val PREF_SYSTEM_WALLPAPER = "system_wallpaper"
    private const val PREF_SYSTEM_WALLPAPER_ALPHA = "system_wallpaper_alpha"
    private const val PREF_DARK_THEME = "dark_theme"
    private const val PREF_BLACK_DARK_THEME = "black_dark_theme"
    private const val PREF_FOLLOW_SYSTEM_ACCENT = "follow_system_accent"
    private const val PREF_THEME_COLOR = "theme_color"

    private const val PREF_BYPASS_RISKY_PACKAGE_WARNING = "bypass_risky_package_warning"

    private const val PREF_APP_FILTER_SHOW_SYSTEM = "app_filter_show_system"
    private const val PREF_APP_FILTER_SORT_METHOD = "app_filter_sort_method"
    private const val PREF_APP_FILTER_REVERSE_ORDER = "app_filter_reverse_order"
    private const val PREF_LOG_FILTER_LEVEL = "log_filter_level"
    private const val PREF_LOG_FILTER_REVERSE_ORDER = "log_filter_reverse_order"

    enum class SortMethod {
        BY_LABEL, BY_PACKAGE_NAME, BY_INSTALL_TIME, BY_UPDATE_TIME
    }

    private val pref = hmaApp.getSharedPreferences("settings", MODE_PRIVATE)
    val isLauncherIconInvisible = MutableSharedFlow<Boolean>(replay = 1)

    var lastVersion: Int
        get() = pref.getInt(PREF_LAST_VERSION, 0)
        set(value) = pref.edit { putInt(PREF_LAST_VERSION, value) }

    var locale: String
        get() = pref.getString(PREF_LOCALE, "SYSTEM")!!
        set(value) = pref.edit { putString(PREF_LOCALE, value) }

    var darkTheme: Int
        get() = pref.getInt(PREF_DARK_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = pref.edit { putInt(PREF_DARK_THEME, value) }

    var systemWallpaper: Boolean
        get() = pref.getBoolean(PREF_SYSTEM_WALLPAPER, false)
        set(value) = pref.edit { putBoolean(PREF_SYSTEM_WALLPAPER, value) }

    var systemWallpaperAlpha: Int
        get() = pref.getInt(PREF_SYSTEM_WALLPAPER_ALPHA, 0xAA)
        set(value) = pref.edit { putInt(PREF_SYSTEM_WALLPAPER_ALPHA, value) }

    var blackDarkTheme: Boolean
        get() = pref.getBoolean(PREF_BLACK_DARK_THEME, false)
        set(value) = pref.edit { putBoolean(PREF_BLACK_DARK_THEME, value) }

    var followSystemAccent: Boolean
        get() = pref.getBoolean(PREF_FOLLOW_SYSTEM_ACCENT, true)
        set(value) = pref.edit { putBoolean(PREF_FOLLOW_SYSTEM_ACCENT, value) }

    var themeColor: String
        get() = pref.getString(PREF_THEME_COLOR, "MATERIAL_BLUE")!!
        set(value) = pref.edit { putString(PREF_THEME_COLOR, value) }

    var hideIcon: Boolean
        get() = isLauncherIconInvisible.get()
        set(value) {
            val enabled = findEnabledAppComponent(hmaApp)
            if (value && enabled != null) {
                hmaApp.packageManager.setComponentEnabledSetting(
                    enabled,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else if (!value && enabled == null) {
                hmaApp.packageManager.setComponentEnabledSetting(
                    ComponentName(hmaApp, AppConstants.COMPONENT_NAME_DEFAULT),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            runBlocking { isLauncherIconInvisible.emit(value) }
        }

    var bypassRiskyPackageWarning: Boolean
        get() = pref.getBoolean(PREF_BYPASS_RISKY_PACKAGE_WARNING, false)
        set(value) = pref.edit { putBoolean(PREF_BYPASS_RISKY_PACKAGE_WARNING, value) }

    var appFilter_showSystem: Boolean
        get() = pref.getBoolean(PREF_APP_FILTER_SHOW_SYSTEM, false)
        set(value) = pref.edit { putBoolean(PREF_APP_FILTER_SHOW_SYSTEM, value) }

    var appFilter_sortMethod: SortMethod
        get() = SortMethod.entries[pref.getInt(PREF_APP_FILTER_SORT_METHOD, SortMethod.BY_LABEL.ordinal)]
        set(value) = pref.edit { putInt(PREF_APP_FILTER_SORT_METHOD, value.ordinal) }

    var appFilter_reverseOrder: Boolean
        get() = pref.getBoolean(PREF_APP_FILTER_REVERSE_ORDER, false)
        set(value) = pref.edit { putBoolean(PREF_APP_FILTER_REVERSE_ORDER, value) }

    var logFilter_level: Int
        get() = pref.getInt(PREF_LOG_FILTER_LEVEL, 0)
        set(value) = pref.edit { putInt(PREF_LOG_FILTER_LEVEL, value) }

    var logFilter_reverseOrder: Boolean
        get() = pref.getBoolean(PREF_LOG_FILTER_REVERSE_ORDER, false)
        set(value) = pref.edit { putBoolean(PREF_LOG_FILTER_REVERSE_ORDER, value) }
}
