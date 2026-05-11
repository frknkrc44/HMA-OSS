package org.frknkrc44.hma_oss.zygote.util

import android.app.ActivityThread
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.os.SystemProperties
import com.android.apksig.ApkVerifier
import com.v7878.unsafe.Reflection.getDeclaredField
import com.v7878.unsafe.Reflection.getDeclaredMethod
import com.v7878.unsafe.invoke.EmulatedStackFrame
import com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.Magic
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.service.SystemServerHook
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method


object Utils4Zygote {
    const val TAG = "Utils4Zygote"

    fun dumpArgs(frame: EmulatedStackFrame, skipFirst: Boolean = false): Array<Any?> {
        return mutableListOf<Any?>().let {
            val begin = if (skipFirst) 1 else 0
            for (index in begin ..< frame.type().parameterCount()) {
                it.add(getArgument(frame, index))
            }

            it.toTypedArray()
        }
    }

    fun getArgument(frame: EmulatedStackFrame, index: Int): Any {
        val accessor = frame.accessor()

        return when (accessor.getArgumentShorty(index)) {
            'L' -> accessor.getReference(index)
            'Z' -> accessor.getBoolean(index)
            'B' -> accessor.getByte(index)
            'C' -> accessor.getChar(index)
            'S' -> accessor.getShort(index)
            'I' -> accessor.getInt(index)
            'J' -> accessor.getLong(index)
            'F' -> accessor.getFloat(index)
            'D' -> accessor.getDouble(index)
            else -> throw Exception("Should not reach here")
        }
    }

    fun setArgument(frame: EmulatedStackFrame, index: Int, value: Any) {
        val accessor = frame.accessor()

        when (accessor.getArgumentShorty(index)) {
            'L' -> accessor.setReference(index, value)
            'Z' -> accessor.setBoolean(index, value as Boolean)
            'B' -> accessor.setByte(index, value as Byte)
            'C' -> accessor.setChar(index, value as Char)
            'S' -> accessor.setShort(index, value as Short)
            'I' -> accessor.setInt(index, value as Int)
            'J' -> accessor.setLong(index, value as Long)
            'F' -> accessor.setFloat(index, value as Float)
            'D' -> accessor.setDouble(index, value as Double)
            else -> throw Exception("Should not reach here")
        }
    }

    fun setReturnValue(frame: EmulatedStackFrame, value: Any?) {
        if (frame.type().returnType() != Void::class.java) {
            frame.accessor().setValue(RETURN_VALUE_IDX, value)
        }
    }

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

    fun getStaticIntField(className: String, name: String) = getDeclaredField(
        Class.forName(className),
        name,
    ).getInt(null)

    fun getIntField(obj: Any, name: String) = getDeclaredField(obj.javaClass, name).getInt(obj)

    fun getBooleanField(obj: Any, name: String) = getDeclaredField(obj.javaClass, name).getBoolean(obj)

    fun getObjectField(obj: Any, name: String): Any? = getDeclaredField(obj.javaClass, name).get(obj)

    fun setBooleanField(obj: Any, name: String, value: Boolean) {
        val field = getDeclaredField(obj.javaClass, name).apply { isAccessible = true }
        field.setBoolean(obj, value)
    }

    fun callMethod(obj: Any, name: String, vararg args: Any): Any? {
        return getDeclaredMethod(
            obj.javaClass,
            name,
            *args.map { it.javaClass }.toTypedArray()
        ).apply { isAccessible = true }.invoke(obj, *args)
    }

    fun callStaticMethod(clazz: Class<*>, name: String, vararg args: Any): Any? {
        return getDeclaredMethod(
            clazz,
            name,
            *args.map { it.javaClass }.toTypedArray()
        ).apply { isAccessible = true }.invoke(null, *args)
    }

    fun findConstructor(className: String, paramCount: Int = -1): Constructor<*>? {
        val clazz = Class.forName(className, true, SystemServerHook.classLoader)

        return clazz.constructors.firstOrNull {
            paramCount == -1 || it.parameterCount == paramCount
        }
    }

    fun findMethod(className: String, name: String, isDeclared: Boolean = false, systemClassLoader: Boolean = false, vararg args: Class<*>): Method {
        val clazz = if (systemClassLoader) Class.forName(className, true, SystemServerHook.classLoader) else Class.forName(className)
        return if (isDeclared) {
            clazz.getDeclaredMethod(name, *args)
        } else {
            clazz.getMethod(name, *args)
        }
    }

    fun findField(clazz: Class<*>, name: String): Field? {
        var currentClazz: Class<*> = clazz
        var field: Field? = null

        while (field == null && currentClazz.javaClass.simpleName != "Object") {
            field = runCatching { currentClazz.getField(name) }.getOrNull()
            currentClazz = clazz.superclass.javaClass
        }

        return field
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
