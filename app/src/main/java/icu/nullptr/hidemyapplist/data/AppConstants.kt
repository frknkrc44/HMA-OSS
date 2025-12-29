package icu.nullptr.hidemyapplist.data

import org.frknkrc44.hma_oss.BuildConfig

object AppConstants {
    const val COMPONENT_NAME_DEFAULT         = "${BuildConfig.APPLICATION_ID}.MainActivityLauncher"
    private const val COMPONENT_NAME_ALT     = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt"
    private const val COMPONENT_NAME_ALT_2   = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt2"
    private const val COMPONENT_NAME_ALT_3   = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt3"

    val allAppIcons = listOf(
        COMPONENT_NAME_DEFAULT,
        COMPONENT_NAME_ALT,
        COMPONENT_NAME_ALT_2,
        COMPONENT_NAME_ALT_3,
    )

    const val UPDATE_CHECK_URL = "https://api.github.com/repos/frknkrc44/HMA-OSS/releases/latest"
}
