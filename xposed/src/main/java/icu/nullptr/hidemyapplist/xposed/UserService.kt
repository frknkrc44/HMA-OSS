package icu.nullptr.hidemyapplist.xposed

import android.app.ActivityManagerHidden
import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import android.os.ServiceManager
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.common.BuildConfig
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.adapter.UidObserverAdapter

object UserService {

    private const val TAG = "HMA-UserService"

    private var appUid = 0

    private val uidObserver = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            if (HMAService.instance == null) {
                logE(TAG, "HMAService instance is not available, maybe stopped")
                return
            }

            if (uid != appUid) return
            try {
                val provider = ActivityManagerApis.getContentProviderExternal(Constants.PROVIDER_AUTHORITY, 0, null, null)
                assert (provider != null) {
                    "Failed to get provider"
                }
                val extras = Bundle()
                extras.putBinder("binder", HMAService.instance)
                val reply = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val attr = AttributionSource.Builder(1000).setPackageName("android").build()
                    provider?.call(attr, Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    provider?.call("android", null, Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else {
                    provider?.call("android", Constants.PROVIDER_AUTHORITY, "", null, extras)
                }
                if (reply == null) {
                    logE(TAG, "Failed to send binder to app")
                    return
                }
                logI(TAG, "Send binder to app")
            } catch (e: Throwable) {
                logE(TAG, "onUidActive", e)
            }
        }
    }

    fun register(pms: IPackageManager, pmn: Any?) {
        logI(TAG, "Initialize HMAService - Version ${BuildConfig.APP_VERSION_NAME}")
        val service = HMAService(pms, pmn)

        try {
            appUid = Utils.getPackageUidCompat(service.pms, BuildConfig.APP_PACKAGE_NAME, 0, 0)
            assert(appUid >= 0) {
                "App UID cannot be -1 or lower"
            }
        } catch (e: Throwable) {
            logE(TAG, "Fatal: Cannot get package details\nCompile this app from source with your changes", e)
            return
        }

        logD(TAG, "Client uid: $appUid")
        logI(TAG, "Register observer")

        waitSystemService("activity")
        ActivityManagerApis.registerUidObserver(
            uidObserver,
            ActivityManagerHidden.UID_OBSERVER_ACTIVE,
            ActivityManagerHidden.PROCESS_STATE_TOP,
            null
        )
    }

    private fun waitSystemService(name: String) {
        while (ServiceManager.getService(name) == null) {
            Thread.sleep(1000)
        }
    }
}
