package icu.nullptr.hidemyapplist.data

import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R

object AppConstants {
    const val COMPONENT_NAME_DEFAULT         = "${BuildConfig.APPLICATION_ID}.MainActivityLauncher"
    private const val COMPONENT_NAME_ALT     = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt"
    private const val COMPONENT_NAME_ALT_2   = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt2"
    private const val COMPONENT_NAME_ALT_3   = "${BuildConfig.APPLICATION_ID}.MainActivityLauncherAlt3"

    val allAppIcons = listOf(
        R.mipmap.ic_launcher       to COMPONENT_NAME_DEFAULT,
        R.mipmap.ic_launcher_alt   to COMPONENT_NAME_ALT,
        R.mipmap.ic_launcher_alt_2 to COMPONENT_NAME_ALT_2,
        R.mipmap.ic_launcher_alt_3 to COMPONENT_NAME_ALT_3,
    )

    const val UPDATE_CHECK_URL = "https://api.github.com/repos/frknkrc44/HMA-OSS/releases/latest"
}
