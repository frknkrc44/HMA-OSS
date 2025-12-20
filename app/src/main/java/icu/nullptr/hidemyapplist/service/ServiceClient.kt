package icu.nullptr.hidemyapplist.service

import android.os.IBinder
import android.util.Log
import icu.nullptr.hidemyapplist.common.IHMAService
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ServiceClient : IHMAService, IBinder.DeathRecipient {

    private const val TAG = "ServiceClient"

    private class ServiceProxy(private val obj: IHMAService) : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            val result = method.invoke(obj, *args.orEmpty())
            if (result == null) Log.i(TAG, "Call service method ${method.name}")
            else Log.i(TAG, "Call service method ${method.name} with result " + result.toString().take(20))
            return result
        }
    }

    @Volatile
    private var service: IHMAService? = null

    fun linkService(binder: IBinder) {
        service = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(IHMAService::class.java),
            ServiceProxy(IHMAService.Stub.asInterface(binder))
        ) as IHMAService
        binder.linkToDeath(this, 0)
    }

    override fun binderDied() {
        service = null
        Log.e(TAG, "Binder died")
    }

    override fun asBinder() = service?.asBinder()

    override fun getServiceVersion() = service?.serviceVersion ?: 0

    override fun getFilterCount() = service?.filterCount ?: 0

    override fun getLogs() = service?.logs

    override fun clearLogs() {
        service?.clearLogs()
    }

    override fun handlePackageEvent(eventType: String?, packageName: String?) {
        service?.handlePackageEvent(eventType, packageName)
    }

    override fun getPackagesForPreset(presetName: String) =
        service?.getPackagesForPreset(presetName)

    override fun readConfig() = service?.readConfig()

    override fun writeConfig(json: String) {
        service?.writeConfig(json)
    }

    override fun stopService(cleanEnv: Boolean) {
        service?.stopService(cleanEnv)
    }

    fun forceStop(packageName: String) {
        forceStop(packageName, 0)
    }

    override fun forceStop(packageName: String, userId: Int) {
        service?.forceStop(packageName, userId)
    }

    override fun log(level: Int, tag: String, message: String) {
        service?.log(level, tag, message)
    }

    override fun getPackageNames(userId: Int) = service?.getPackageNames(userId)

    override fun getPackageInfo(
        packageName: String?,
        userId: Int
    ) = service?.getPackageInfo(packageName, userId)

    override fun listAllSettings(databaseName: String) = service?.listAllSettings(databaseName) ?: arrayOf()

    override fun getLogFileLocation() = service?.logFileLocation ?: "the log file"
}