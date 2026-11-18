package org.frknkrc44.hma_oss.zygote.service

import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import icu.nullptr.hidemyapplist.common.Constants.PROVIDER_AUTHORITY
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.util.ActivityManagerUtils.getContentProviderExternal
import org.frknkrc44.hma_oss.zygote.util.ActivityManagerUtils.registerUidObserver
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService
import org.frknkrc44.hma_oss.zygote.util.UidObserverAdapter
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField

object UserService {

    private const val TAG = "HMA-UserService"

    private val managerAppUid get() = service?.appUid ?: -1

    var service: HMAService? = null

    private val uidObserver = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            if (managerAppUid < 0 || uid != managerAppUid) {
                return
            }

            try {
                val userId = getUserFromCallingUid(uid)

                logD(TAG) { "Calculated user id: $userId" }

                val provider = getContentProviderExternal(PROVIDER_AUTHORITY, userId)
                assert (provider != null) {
                    "Failed to get provider"
                }
                val extras = Bundle()
                extras.putBinder("binder", service)
                val reply = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val attr = AttributionSource.Builder(1000).setPackageName("android").build()
                    provider?.call(attr, PROVIDER_AUTHORITY, "", null, extras)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    provider?.call("android", null, PROVIDER_AUTHORITY, "", null, extras)
                } else {
                    provider?.call("android", PROVIDER_AUTHORITY, "", null, extras)
                }
                if (reply == null) {
                    logE(TAG) { "Failed to send binder to app" }
                    return
                }
                logI(TAG) { "Send binder to app" }
            } catch (e: Throwable) {
                logE(TAG, e) { "onUidActive" }
            }
        }
    }

    fun register(pms: IPackageManager, pmn: Any?) {
        assert(service == null) { "You cannot register the service more than once" }

        logI(TAG) { "Initialize HMAService - Version ${BuildConfig.APP_VERSION_NAME}" }

        waitForService("activity")
        registerUidObserver(
            uidObserver,
            getActMgrField("UID_OBSERVER_ACTIVE"),
            getActMgrField("PROCESS_STATE_TOP"),
        )

        logI(TAG) { "Registered observer" }

        HMAService(pms, pmn)
    }

    private fun getActMgrField(name: String) = getStaticIntField(
        "android.app.ActivityManager",
        name,
    )
}
