package org.frknkrc44.hma_oss.zygote.util

import android.os.Build
import android.os.ServiceManager
import android.provider.Settings
import android.webkit.IWebViewUpdateService
import com.android.server.pm.PackageManagerService
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import org.frknkrc44.hma_oss.zygote.util.ContextUtils.contentResolver
import org.frknkrc44.hma_oss.zygote.util.ContextUtils.packageManager
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethodWithTypes
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_PROVIDER_KEY
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.WEBVIEW_UPDATE_SERVICE

object BrowserUtils {
    const val TAG = "BrowserUtils"

    @Volatile
    private var useAltMethodForBrowserCheck = false

    fun getDefaultBrowser(pmn: Any?, userId: Int): String? {
        if (!useAltMethodForBrowserCheck) {
            val pmnMethod = getDefaultBrowserPMN(pmn, userId)

            return if (useAltMethodForBrowserCheck) {
                getDefaultBrowserPM(userId)
            } else {
                pmnMethod
            }
        }

        return getDefaultBrowserPM(userId)
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

    /**
     * This method is mainly called on non-Samsung devices
     */
    private fun getDefaultBrowserPMN(pmn: Any?, userId: Int): String? {
        return try {
            val pms = getObjectField(
                pmn ?: return null,
                "mPm",
            ) as? PackageManagerService ?: return null

            when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.Q -> pms.getDefaultBrowserPackageName(userId)
                Build.VERSION_CODES.R -> pms.getPermissionManagerServiceInternal().getDefaultBrowser(userId)
                else -> pms.defaultAppProvider.getDefaultBrowser(userId)
            }
        } catch (e: Throwable) {
            logD(TAG, e) { "Getting default browser failed through PMN" }

            useAltMethodForBrowserCheck = true

            null
        }
    }

    /**
     * This method is mainly called on Samsung devices
     */
    private fun getDefaultBrowserPM(userId: Int): String? {
        return try {
            callMethodWithTypes(
                packageManager,
                "getDefaultBrowserPackageNameAsUser",
                arrayOf(Int::class.javaPrimitiveType!!),
                arrayOf(userId),
            ) as? String
        } catch (x: Throwable) {
            logD(TAG, x) { "Getting default browser failed through PM" }

            null
        }
    }

    private val webViewService by lazy {
        IWebViewUpdateService.Stub.asInterface(
            ServiceManager.getService(WEBVIEW_UPDATE_SERVICE)
        )
    }
}
