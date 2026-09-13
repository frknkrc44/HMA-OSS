package icu.nullptr.hidemyapplist.service

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import icu.nullptr.hidemyapplist.MyApp.Companion.hmaApp
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ServiceClient : IHMAService, IBinder.DeathRecipient {

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

    val logs get(): String {
        val parcelFD = readFD(Constants.PARCEL_TYPE_LOG) ?: return ""
        val readStream = FileInputStream(parcelFD.fileDescriptor)
        return readStream.readBytes().decodeToString().also {
            readStream.close()
            parcelFD.close()
        }
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
            val parcelFD = service?.readFD(Constants.PARCEL_TYPE_CONFIG) ?: return "{}"
            val readStream = FileInputStream(parcelFD.fileDescriptor)
            return readStream.readBytes().decodeToString().also {
                readStream.close()
                parcelFD.close()
            }
        }
        set(text) {
            val configFile = File("${hmaApp.filesDir.absolutePath}/temp_config.json")
            configFile.writeText(text)

            val parcelFD = ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY)
            writeFD(Constants.PARCEL_TYPE_CONFIG, parcelFD)
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
}
