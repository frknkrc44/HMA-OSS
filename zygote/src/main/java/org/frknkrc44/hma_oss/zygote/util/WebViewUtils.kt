package org.frknkrc44.hma_oss.zygote.util

import android.os.ServiceManager
import android.provider.Settings
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.contentResolver
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod

object WebViewUtils {
    private val webViewService by lazy { ServiceManager.getService("webviewupdate") }

    fun getWebviewProvider(): String? = binderLocalScope {
        try {
            (callMethod(webViewService, "getCurrentWebViewPackageName") as? String).also { assert(it != null) }
        } catch (_: Throwable) {
            Settings.Global.getString(contentResolver, "webview_provider")
        }
    }
}
