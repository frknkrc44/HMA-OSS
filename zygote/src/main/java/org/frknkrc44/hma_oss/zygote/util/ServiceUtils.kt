package org.frknkrc44.hma_oss.zygote.util

import android.app.ActivityThread
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import com.android.apksig.ApkVerifier
import com.v7878.unsafe.Reflection.getDeclaredMethod
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.Magic
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findField
import java.io.File


object ServiceUtils {
    const val TAG = "ServiceUtils"

    @Throws(InterruptedException::class)
    fun waitForService(name: String?): IBinder? {
        try {
            return getDeclaredMethod(
                ServiceManager::class.java,
                "waitForService",
                String::class.java,
            ).invoke(null, name) as IBinder?
        } catch (e: Throwable) {
            logE(TAG, e) { "An error occurred on waitForService" }
        }

        var service: IBinder? = null

        do {
            Thread.sleep(250)
        } while ((ServiceManager.getService(name).also { service = it }) == null)

        return service
    }

    fun getPackageNameFromPackageSettings(packageSettings: Any?): String? {
        if (packageSettings == null) return null

        return try {
            callMethod(packageSettings, "getPackageName") as String?
        } catch (_: Throwable) {
            runCatching {
                findField(
                    packageSettings::class.java,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "mName" else "name"
                )?.apply { isAccessible = true }?.get(packageSettings) as? String
            }.getOrNull()
        }
    }

    fun getPackageManager() = ActivityThread.currentActivityThread().application.packageManager!!

    fun getCallingApps(service: HMAService): Array<String> {
        return getCallingApps(service, Binder.getCallingUid())
    }

    fun getCallingApps(service: HMAService, callingUid: Int): Array<String> {
        if (callingUid == Constants.UID_SYSTEM) return arrayOf()
        return Utils.binderLocalScope {
            service.pms.getPackagesForUid(callingUid)
        } ?: arrayOf()
    }

    fun verifyAppSignature(path: String?): Boolean {
        if (path == null) return false

        val verifier = ApkVerifier.Builder(File(path))
            .setMinCheckedPlatformVersion(24)
            .build()
        val result = verifier.verify()
        if (!result.isVerified) return false
        val mainCert = result.signerCertificates[0]
        return mainCert.encoded.contentEquals(Magic.magicNumbers)
    }

    fun clearStackTraces(throwableIn: Throwable) {
        var throwable: Throwable? = throwableIn

        while (throwable != null) {
            val newTrace = throwable.stackTrace.filter { item ->
                !Utils.containsMultiple(
                    item.className,
                    "BulkHooker",
                    "com.v7878",
                    "MethodHandle",
                    BuildConfig.APP_PACKAGE_NAME,
                ) && !Utils.containsMultiple(
                    item.fileName,
                    "r8-map-id-",
                    "dex-id-",
                )
            }

            if (newTrace.size != throwable.stackTrace.size) {
                throwable.stackTrace = newTrace.toTypedArray()
            }

            throwable = throwable.cause
        }
    }
}
