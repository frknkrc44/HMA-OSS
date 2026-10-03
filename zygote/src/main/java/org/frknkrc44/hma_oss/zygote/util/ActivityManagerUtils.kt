package org.frknkrc44.hma_oss.zygote.util

import android.app.IActivityManager
import android.app.IUidObserver
import android.content.Context
import android.content.IContentProvider
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.system.Os
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService


object ActivityManagerUtils {
    private val activityService by lazy { waitForService(Context.ACTIVITY_SERVICE) as? IActivityManager }

    @Throws(RemoteException::class)
    fun forceStopPackage(packageName: String, userId: Int) {
        activityService?.forceStopPackage(packageName, userId)
    }

    @Throws(RemoteException::class)
    fun startActivity(intent: Intent?, userId: Int) {
        activityService?.startActivityAsUser(
            null,
            if (Os.getuid() == 2000) "com.android.shell" else null,
            intent,
            null,
            null,
            null,
            0,
            0,
            null,
            null,
            userId,
        )
    }

    @Throws(RemoteException::class)
    fun registerUidObserver(observer: IUidObserver?, which: Int, cutpoint: Int, callingPackage: String?) {
        activityService?.registerUidObserver(observer, which, cutpoint, callingPackage)
    }

    fun getContentProviderExternal(name: String?, userId: Int, token: IBinder?, tag: String?): IContentProvider? {
        return activityService?.getContentProviderExternal(name, userId, token, tag)?.provider
    }
}
