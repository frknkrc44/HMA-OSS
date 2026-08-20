package org.frknkrc44.hma_oss.zygote.util

import android.os.Build
import android.os.ServiceManager
import android.provider.Settings
import android.webkit.IWebViewUpdateService
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.contentResolver
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_PROVIDER_KEY
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_UPDATE_SERVICE

object WebViewUtils {
    private val webViewService by lazy {
        ServiceManager.getService(WEBVIEW_UPDATE_SERVICE) as IWebViewUpdateService
    }

    fun getWebviewProvider(): String? = binderLocalScope {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                webViewService.currentWebViewPackage?.packageName ?: throw NullPointerException()
            } else {
                webViewService.currentWebViewPackageName ?: throw NullPointerException()
            }
        } catch (_: Throwable) {
            Settings.Global.getString(contentResolver, WEBVIEW_PROVIDER_KEY)
        }
    }
}
