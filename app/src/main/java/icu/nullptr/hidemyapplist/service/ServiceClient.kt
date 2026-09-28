package icu.nullptr.hidemyapplist.service

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.util.FDUtils.readFromPipe
import icu.nullptr.hidemyapplist.util.FDUtils.writeIntoPipe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ServiceClient : IHMAService {

    private const val TAG = "ServiceClient"

    private class ServiceProxy(private val obj: IHMAService) : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            val result = method.invoke(obj, *args.orEmpty())
            if (result == null) Log.d(TAG, "Call service method ${method.name}")
            else Log.d(TAG, "Call service method ${method.name} with result " + result.toString().take(20))
            return result
        }
    }

    @Volatile
    private var service: IHMAService? = null
    private var provider: ServiceProvider? = null

    private var activeBinder: IBinder? = null
    private var deathRecipient: IBinder.DeathRecipient? = null

    @Volatile
    var backend: String? = null
        private set

    @Volatile
    var backendApiVersion: Int? = null
        private set

    @Synchronized
    fun linkService(
        provider: ServiceProvider,
        binder: IBinder,
        backendName: String = "zygisk",
        apiVersion: Int? = null,
    ) {
        this.provider = provider
        if (activeBinder === binder && service != null) {
            backend = backendName
            backendApiVersion = apiVersion
            return
        }

        val nextService = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(IHMAService::class.java),
            ServiceProxy(IHMAService.Stub.asInterface(binder))
        ) as IHMAService

        val recipient = IBinder.DeathRecipient {
            val wasActive = synchronized(this) {
                if (activeBinder !== binder) false else {
                    service = null
                    backend = null
                    backendApiVersion = null
                    activeBinder = null
                    deathRecipient = null
                    true
                }
            }
            if (wasActive) Log.e(TAG, "Binder died")
        }
        binder.linkToDeath(recipient, 0)

        val oldBinder = activeBinder
        val oldRecipient = deathRecipient
        // Publish the label before the volatile service reference: the UI polls
        // serviceVersion and must not observe a new service with stale metadata.
        backend = backendName
        backendApiVersion = apiVersion
        activeBinder = binder
        deathRecipient = recipient
        service = nextService
        if (oldBinder != null && oldRecipient != null) {
            runCatching { oldBinder.unlinkToDeath(oldRecipient, 0) }
        }
        Log.i(TAG, "Connected backend: $backendName")
    }

    override fun asBinder() = service?.asBinder()

    override fun getServiceVersion() = service?.serviceVersion ?: 0

    override fun getFilterCount() = service?.filterCount ?: 0

    val logs get(): String {
        val descriptor = readFD(Constants.PARCEL_TYPE_LOG) ?: return ""
        return readFromPipe(descriptor)
    }

    override fun clearLogs() {
        service?.clearLogs()
    }

    override fun handlePackageEvent(eventType: String?, packageName: String?, extras: Bundle?) {
        service?.handlePackageEvent(eventType, packageName, extras)
    }

    override fun getPackagesForPreset(presetName: String) =
        service?.getPackagesForPreset(presetName)

    var config: String
        get() {
            val descriptor = service?.readFD(Constants.PARCEL_TYPE_CONFIG) ?: return "{}"
            return readFromPipe(descriptor)
        }
        set(text) = writeFD(
            Constants.PARCEL_TYPE_CONFIG,
            writeIntoPipe(provider ?: return, text),
        )

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

    override fun reloadPresetsFromScratch() {
        service?.reloadPresetsFromScratch()
    }

    override fun getDetailedFilterStats() = service?.detailedFilterStats

    override fun clearFilterStats() {
        service?.clearFilterStats()
    }

    /**
     * Get the current service `BuildConfig.APP_VERSION_NAME`.
     * Returns `null` if there is no service connection or an old version is installed.
     */
    override fun getServiceVersionName() = try {
        service?.serviceVersionName
    } catch (_: Throwable) { null }

    override fun getLoadedHooks() = service?.loadedHooks

    override fun readFD(type: Int) = service?.readFD(type)

    override fun writeFD(type: Int, fd: ParcelFileDescriptor) {
        service?.writeFD(type, fd)
    }

    override fun getManagerWorkMode() = service?.managerWorkMode ?: Constants.MANAGER_WORK_MODE_UNKNOWN

    override fun startMainActivityAsUser(packageName: String, userId: Int) {
        service?.startMainActivityAsUser(packageName, userId)
    }

    override fun migrateData(packageName: String) = service?.migrateData(packageName) ?: false

    override fun reloadConfigFromFile() {
        service?.reloadConfigFromFile()
        ConfigManager.init()
    }

    override fun getUserProfiles() = service?.userProfiles
}
