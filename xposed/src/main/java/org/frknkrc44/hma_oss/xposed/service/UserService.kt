package org.frknkrc44.hma_oss.xposed.service

import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import icu.nullptr.hidemyapplist.common.BackendRegistry
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.xposed.util.Logcat.logE
import org.frknkrc44.hma_oss.xposed.util.Logcat.logI
import org.frknkrc44.hma_oss.xposed.util.ZLUtils.getStaticIntField
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.adapter.UidObserverAdapter

object UserService {
    @Volatile
    var service: HMAService? = null

    private val observer = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            val current = service ?: return
            if (uid != current.appUid || uid < 0) return
            try {
                val provider = ActivityManagerApis.getContentProviderExternal(
                    Constants.PROVIDER_AUTHORITY, getUserFromCallingUid(uid), null, null
                ) ?: return
                val extras = Bundle().apply {
                    putBinder("binder", current)
                    putString(BackendRegistry.EXTRA_BACKEND, "xposed")
                    putInt(BackendRegistry.EXTRA_API_VERSION, SystemServerHook.framework.apiVersion)
                }
                val reply = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    provider.call(AttributionSource.Builder(1000).setPackageName("android").build(),
                        Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    provider.call("android", null, Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else {
                    provider.call("android", Constants.PROVIDER_AUTHORITY, "", null, extras)
                }
                checkNotNull(reply) { "Manager rejected service" }
                logI(TAG) { "Sent libxposed service to manager uid=$uid" }
            } catch (error: Throwable) {
                logE(TAG, error) { "Failed to deliver service" }
            }
        }
    }

    @Synchronized
    fun register(pms: IPackageManager, pmn: Any?) {
        check(service == null) { "Service already registered" }
        val current = HMAService(pms, pmn)
        check(current.appUid >= 0) { "Manager signature verification failed" }
        ActivityManagerApis.registerUidObserver(observer,
            getStaticIntField("android.app.ActivityManager", "UID_OBSERVER_ACTIVE"),
            getStaticIntField("android.app.ActivityManager", "PROCESS_STATE_TOP"), null)
        // Cover a manager already active when initialization finishes.
        observer.onUidActive(current.appUid)
    }

    private const val TAG = "HMA-XposedService"
}
