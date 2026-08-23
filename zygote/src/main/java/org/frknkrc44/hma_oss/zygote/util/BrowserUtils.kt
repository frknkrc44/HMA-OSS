package org.frknkrc44.hma_oss.zygote.util

import android.os.Build
import android.os.ServiceManager
import android.provider.Settings
import android.webkit.IWebViewUpdateService
import com.android.server.pm.PackageManagerService
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.contentResolver
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_PROVIDER_KEY
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_UPDATE_SERVICE

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

    fun getWebviewProvider(): String? = binderLocalScope {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                webViewService.currentWebViewPackage?.packageName
            } else {
                webViewService.currentWebViewPackageName
            }
        } catch (_: Throwable) {
            Settings.Global.getString(contentResolver, WEBVIEW_PROVIDER_KEY)
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

    private val webViewService by lazy {
        ServiceManager.getService(WEBVIEW_UPDATE_SERVICE) as IWebViewUpdateService
    }
}
