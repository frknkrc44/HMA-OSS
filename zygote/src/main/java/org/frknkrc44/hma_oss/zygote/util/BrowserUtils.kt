package org.frknkrc44.hma_oss.zygote.util

import android.os.Build
import com.android.server.pm.PackageManagerService
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField

object BrowserUtils {
    const val TAG = "BrowserUtils"

    fun getDefaultBrowser(userId: Int): String? {
        return try {
            getDefaultBrowserPMN(userId)
        } catch (e: Throwable) {
            logD(TAG, e) { "Getting default browser failed" }
            null
        }
    }

    private fun getDefaultBrowserPMN(userId: Int): String? {
        val pms = getObjectField(
            service?.pmn ?: return null,
            "mPm",
        ) as? PackageManagerService ?: return null

        return when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> pms.getDefaultBrowserPackageName(userId)
            Build.VERSION_CODES.R -> pms.getPermissionManagerServiceInternal().getDefaultBrowser(userId)
            else -> pms.defaultAppProvider.getDefaultBrowser(userId)
        }
    }
}
