package icu.nullptr.hidemyapplist.ui.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import icu.nullptr.hidemyapplist.service.PrefManager
import org.frknkrc44.hma_oss.R

object ThemeUtils {
    private val colorThemeMap = mapOf(
        "SAKURA" to R.style.ThemeOverlay_Light_MaterialSakura,
        "MATERIAL_RED" to R.style.ThemeOverlay_Light_MaterialRed,
        "MATERIAL_PINK" to R.style.ThemeOverlay_Light_MaterialPink,
        "MATERIAL_PURPLE" to R.style.ThemeOverlay_Light_MaterialPurple,
        "MATERIAL_DEEP_PURPLE" to R.style.ThemeOverlay_Light_MaterialDeepPurple,
        "MATERIAL_INDIGO" to R.style.ThemeOverlay_Light_MaterialIndigo,
        "MATERIAL_BLUE" to R.style.ThemeOverlay_Light_MaterialBlue,
        "MATERIAL_LIGHT_BLUE" to R.style.ThemeOverlay_Light_MaterialLightBlue,
        "MATERIAL_CYAN" to R.style.ThemeOverlay_Light_MaterialCyan,
        "MATERIAL_TEAL" to R.style.ThemeOverlay_Light_MaterialTeal,
        "MATERIAL_GREEN" to R.style.ThemeOverlay_Light_MaterialGreen,
        "MATERIAL_LIGHT_GREEN" to R.style.ThemeOverlay_Light_MaterialLightGreen,
        "MATERIAL_LIME" to R.style.ThemeOverlay_Light_MaterialLime,
        "MATERIAL_YELLOW" to R.style.ThemeOverlay_Light_MaterialYellow,
        "MATERIAL_AMBER" to R.style.ThemeOverlay_Light_MaterialAmber,
        "MATERIAL_ORANGE" to R.style.ThemeOverlay_Light_MaterialOrange,
        "MATERIAL_DEEP_ORANGE" to R.style.ThemeOverlay_Light_MaterialDeepOrange,
        "MATERIAL_BROWN" to R.style.ThemeOverlay_Light_MaterialBrown,
        "MATERIAL_BLUE_GREY" to R.style.ThemeOverlay_Light_MaterialBlueGrey
    )

    private val darkColorThemeMap = mapOf(
        "SAKURA" to R.style.ThemeOverlay_Dark_MaterialSakura,
        "MATERIAL_RED" to R.style.ThemeOverlay_Dark_MaterialRed,
        "MATERIAL_PINK" to R.style.ThemeOverlay_Dark_MaterialPink,
        "MATERIAL_PURPLE" to R.style.ThemeOverlay_Dark_MaterialPurple,
        "MATERIAL_DEEP_PURPLE" to R.style.ThemeOverlay_Dark_MaterialDeepPurple,
        "MATERIAL_INDIGO" to R.style.ThemeOverlay_Dark_MaterialIndigo,
        "MATERIAL_BLUE" to R.style.ThemeOverlay_Dark_MaterialBlue,
        "MATERIAL_LIGHT_BLUE" to R.style.ThemeOverlay_Dark_MaterialLightBlue,
        "MATERIAL_CYAN" to R.style.ThemeOverlay_Dark_MaterialCyan,
        "MATERIAL_TEAL" to R.style.ThemeOverlay_Dark_MaterialTeal,
        "MATERIAL_GREEN" to R.style.ThemeOverlay_Dark_MaterialGreen,
        "MATERIAL_LIGHT_GREEN" to R.style.ThemeOverlay_Dark_MaterialLightGreen,
        "MATERIAL_LIME" to R.style.ThemeOverlay_Dark_MaterialLime,
        "MATERIAL_YELLOW" to R.style.ThemeOverlay_Dark_MaterialYellow,
        "MATERIAL_AMBER" to R.style.ThemeOverlay_Dark_MaterialAmber,
        "MATERIAL_ORANGE" to R.style.ThemeOverlay_Dark_MaterialOrange,
        "MATERIAL_DEEP_ORANGE" to R.style.ThemeOverlay_Dark_MaterialDeepOrange,
        "MATERIAL_BROWN" to R.style.ThemeOverlay_Dark_MaterialBrown,
        "MATERIAL_BLUE_GREY" to R.style.ThemeOverlay_Dark_MaterialBlueGrey
    )

    val isSystemAccent get() = DynamicColors.isDynamicColorAvailable() && PrefManager.followSystemAccent

    fun isNightMode(context: Context) = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun isUsingBlackTheme(context: Context) = PrefManager.blackDarkTheme && isNightMode(context)

    @StyleRes
    fun getOverlayThemeStyleRes(context: Context): Int {
        if (isUsingBlackTheme(context)) return R.style.ThemeOverlay_Black
        if (PrefManager.systemWallpaper) return R.style.ThemeOverlay_Wallpaper

        return R.style.ThemeOverlay
    }

    val colorTheme get() = if (isSystemAccent) "SYSTEM" else PrefManager.themeColor

    @StyleRes
    fun getColorThemeStyleRes(context: Context): Int {
        return if (isNightMode(context)) {
            darkColorThemeMap[colorTheme] ?: R.style.ThemeOverlay_Dark_MaterialBlue
        } else {
            colorThemeMap[colorTheme] ?: R.style.ThemeOverlay_Light_MaterialBlue
        }
    }

    /**
     * Retrieve a color from the current [android.content.res.Resources.Theme].
     */
    @ColorInt
    fun Context.themeColor(
        @AttrRes themeAttrId: Int
    ): Int {
        val style = obtainStyledAttributes(intArrayOf(themeAttrId))
        val color = style.getColor(0, Color.MAGENTA)
        style.recycle()
        return color
    }

    @ColorInt
    fun Fragment.themeColor(
        @AttrRes themeAttrId: Int
    ) = requireContext().themeColor(themeAttrId)

    fun Context.attrDrawable(
        @AttrRes themeAttrId: Int
    ): Drawable? {
        val style = obtainStyledAttributes(intArrayOf(themeAttrId))
        val drawable = style.getDrawable(0)
        style.recycle()
        return drawable
    }

    fun Fragment.attrDrawable(
        @AttrRes themeAttrId: Int
    ) = requireContext().attrDrawable(themeAttrId)

    @ColorInt
    fun Fragment.getColor(
        @ColorRes colorId: Int
    ) = requireContext().getColor(colorId)

    fun Context.homeItemBackgroundColor(forceNoTrans: Boolean = false) = (if (isNightMode(this)) {
        themeColor(com.google.android.material.R.attr.colorSurfaceContainerHighest)
    } else {
        themeColor(com.google.android.material.R.attr.colorSurfaceContainer)
    }).let {
        if (!forceNoTrans && PrefManager.systemWallpaper) return@let it - 0x55000000
        return@let it
    }

    fun Fragment.homeItemBackgroundColor(forceNoTrans: Boolean = false) = requireContext().homeItemBackgroundColor(forceNoTrans)

    fun Int.asDrawable(context: Context) = ResourcesCompat.getDrawable(context.resources, this, context.theme)!!
}
