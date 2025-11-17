package org.frknkrc44.hma_oss.ui.activity

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils
import icu.nullptr.hidemyapplist.util.ConfigUtils.Companion.getLocale

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // I add this manually because the E2E code is not working like I want
        // They should give us a separate method to choice it for enableEdgeToEdge
        // Source: https://github.com/androidx/androidx/blob/c0f9aabcf6f32029249ac7647711744b68e2a003/activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt#L299
        window.isNavigationBarContrastEnforced = !PrefManager.systemWallpaper

        DynamicColors.applyToActivityIfAvailable(
            this,
            DynamicColorsOptions.Builder().also {
                if (!ThemeUtils.isSystemAccent)
                    it.setThemeOverlay(ThemeUtils.getColorThemeStyleRes(this))
            }.build()
        )
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        super.onApplyThemeResource(theme, resid, first)
        if (!DynamicColors.isDynamicColorAvailable()) {
            theme.applyStyle(ThemeUtils.getColorThemeStyleRes(this), true)
        }

        theme.applyStyle(ThemeUtils.getOverlayThemeStyleRes(this), true)

        applyWallpaperBackgroundColor()
    }

    fun applyWallpaperBackgroundColor(value: Int = PrefManager.systemWallpaperAlpha) {
        if (PrefManager.systemWallpaper) {
            val color = (value shl 24) + if (ThemeUtils.isNightMode(this)) {
                0x00000000
            } else {
                0x00FFFFFF
            }

            window.setBackgroundDrawable(color.toDrawable())
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(getLocaleAppliedContext(newBase))
    }

    fun getLocaleAppliedContext(context: Context?): Context? {
        val config = hmaApp.resources.configuration
        config.setLocale(getLocale())

        return context?.createConfigurationContext(config)
    }
}