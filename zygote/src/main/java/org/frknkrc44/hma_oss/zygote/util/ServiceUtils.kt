package org.frknkrc44.hma_oss.zygote.util

import android.app.ActivityThread
import android.content.Context.USER_SERVICE
import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.IUserManager
import android.os.ServiceManager
import android.provider.Settings
import com.android.apksig.ApkVerifier
import com.v7878.unsafe.Reflection.getDeclaredMethod
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import icu.nullptr.hidemyapplist.common.Utils.containsMultiple
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.Magic
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethodWithTypes
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findField
import rikka.hidden.compat.UserManagerApis
import java.io.File


object ServiceUtils {
    private const val TAG = "ServiceUtils"

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

    val packageManager get() = ActivityThread.currentActivityThread().application.packageManager!!

    val contentResolver get() = ActivityThread.currentActivityThread().application.contentResolver!!

    fun getCallingApps(service: HMAService): Array<String> {
        return getCallingApps(service, Binder.getCallingUid())
    }

    fun getCallingApps(service: HMAService, callingUid: Int): Array<String> {
        if (callingUid == Constants.UID_SYSTEM) return arrayOf()
        return binderLocalScope {
            service.pms.getPackagesForUid(callingUid)
        } ?: arrayOf()
    }

    fun findAndVerifyAppSignature(pms: IPackageManager): Int {
        val userService = waitForService(USER_SERVICE)

        try {
            val userManager = IUserManager.Stub.asInterface(userService)
            val profiles = mutableSetOf<Int>().also { set ->
                val userIds = UserManagerApis.getUserIdsNoThrow()

                runCatching {
                    userIds.forEach {
                        val profiles = callMethodWithTypes(
                            userManager,
                            "getProfileIds",
                            arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                            arrayOf(it, false),
                        ) ?: return@forEach

                        (profiles as IntArray).forEach { pId -> set.add(pId) }
                    }
                }.onFailure {
                    set.addAll(userIds)
                }
            }

            for (uid in profiles) {
                logV(TAG) { "@findAndVerifyAppSignature: checking for uid $uid" }

                val pkgInfo = runCatching {
                    getPackageInfoCompat(pms, BuildConfig.APP_PACKAGE_NAME, 0L, uid)
                }.getOrNull()

                if (pkgInfo != null) {
                    if (verifyAppSignature(pkgInfo.applicationInfo?.sourceDir)) {
                        val appUid = pkgInfo.applicationInfo!!.uid

                        logI(TAG) { "The manager app signature is verified successfully, uid: $appUid" }

                        return appUid
                    } else {
                        throw AssertionError("The manager app is modified, skipping")
                    }
                }
            }
        } catch (e: Throwable) {
            logE(TAG, e) { "Fatal: Cannot get package details\nCompile this app from source with your changes" }

            return -1
        }

        logE(TAG) { "The manager app is not found, skipping" }

        return -1
    }

    private fun verifyAppSignature(path: String?): Boolean {
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
                !containsMultiple(
                    item.className,
                    "BulkHooker",
                    "com.v7878",
                    "MethodHandle",
                    BuildConfig.APP_PACKAGE_NAME,
                ) && !containsMultiple(
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
