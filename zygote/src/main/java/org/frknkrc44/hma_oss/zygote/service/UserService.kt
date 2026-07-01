package org.frknkrc44.hma_oss.zygote.service

import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.ZygoteEntry
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.isConflictingModuleInstalled
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.adapter.UidObserverAdapter

object UserService {

    private const val TAG = "HMA-UserService"

    private val managerAppUid get() = service?.appUid ?: -1

    private val uidObserver = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            if (managerAppUid < 0 || uid != managerAppUid) {
                return
            }

            try {
                val userId = getUserFromCallingUid(uid)

                logD(TAG) { "Calculated user id: $userId" }

                val provider = ActivityManagerApis.getContentProviderExternal(Constants.PROVIDER_AUTHORITY, userId, null, null)
                assert (provider != null) {
                    "Failed to get provider"
                }
                val extras = Bundle()
                extras.putBinder("binder", service)
                val reply = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val attr = AttributionSource.Builder(1000).setPackageName("android").build()
                    provider?.call(attr, Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    provider?.call("android", null, Constants.PROVIDER_AUTHORITY, "", null, extras)
                } else {
                    provider?.call("android", Constants.PROVIDER_AUTHORITY, "", null, extras)
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
        logI(TAG) { "Initialize HMAService - Version ${BuildConfig.APP_VERSION_NAME}" }

        val managerWorkMode = if (pms.isConflictingModuleInstalled()) {
            logE(ZygoteEntry.TAG) { "Conflicting module detected, skipping hook" }
            Constants.MANAGER_WORK_MODE_NO_HOOKS
        } else {
            Constants.MANAGER_WORK_MODE_OK
        }

        waitForService("activity")
        ActivityManagerApis.registerUidObserver(
            uidObserver,
            getActMgrField("UID_OBSERVER_ACTIVE"),
            getActMgrField("PROCESS_STATE_TOP"),
            null
        )

        logI(TAG) { "Registered observer" }

        // no need to put in a variable
        HMAService(pms, pmn, managerWorkMode)
    }

    private fun getActMgrField(name: String) = getStaticIntField(
        "android.app.ActivityManager",
        name,
    )
}
