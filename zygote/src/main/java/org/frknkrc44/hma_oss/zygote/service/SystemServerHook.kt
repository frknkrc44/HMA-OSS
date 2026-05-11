package org.frknkrc44.hma_oss.zygote.service

import android.annotation.SuppressLint
import android.content.pm.IPackageManager
import android.os.Build
import com.v7878.r8.annotations.DoNotShrink
import com.v7878.unsafe.Reflection.getDeclaredMethod
import com.v7878.unsafe.invoke.EmulatedStackFrame
import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.Hooks
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.isSystemBootCompleted
import kotlin.concurrent.thread

@SuppressLint("PrivateApi")
object SystemServerHook {
    private const val TAG = "SystemServerHook"
    private const val SYSTEM_SERVER: String = "com.android.server.SystemServer"
    private const val RUNTIME_INIT: String = "com.android.internal.os.RuntimeInit"
    private const val ZYGOTE_INIT: String = "com.android.internal.os.ZygoteInit"

    var classLoader: ClassLoader? = null
    var initialized = false

    @Throws(Throwable::class)
    fun onSystemServer(loader: ClassLoader?) {
        logV(TAG) { "Class loader found: $loader" }

        classLoader = loader

        if (!initialized) {
            initialized = true

            thread {
                val pms = Utils4Zygote.waitForService("package") as IPackageManager
                val pmn = Utils4Zygote.waitForService("package_native")
                logD(TAG) { "Got pms: $pms, $pmn" }

                runCatching {
                    UserService.register(pms, pmn)
                    logI(TAG) { "User service started" }
                }.onFailure {
                    logE(TAG, it) { "System service crashed" }
                }
            }
        }
    }

    @Throws(Throwable::class)
    private fun checkSystemServer(frame: EmulatedStackFrame) {
        val accessor = frame.accessor()
        if (SYSTEM_SERVER == accessor.getReference(0)) {
            val loader: ClassLoader? = accessor.getReference(2)
            onSystemServer(loader)
        }
    }

    @DoNotShrink
    @Throws(Throwable::class)
    @JvmStatic
    fun init() {
        // This module was loaded after boot or not
        if (isSystemBootCompleted()) {
            logI(TAG) { "Trying to invoke late-load mode" }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                throw UnsupportedOperationException("This Android version isn't support late-load")
            }

            val method = getDeclaredMethod(
                Class.forName(ZYGOTE_INIT),
                "getOrCreateSystemServerClassLoader"
            )

            onSystemServer(method.invoke(null) as? ClassLoader)
        } else {
            logI(TAG) { "Trying to invoke boot-load mode" }

            val method = getDeclaredMethod(
                Class.forName(RUNTIME_INIT), "findStaticMain",
                String::class.java, Array<String>::class.java, ClassLoader::class.java
            )

            Hooks.hook(method, Hooks.EntryPointType.CURRENT, { original, frame ->
                try {
                    checkSystemServer(frame)
                } catch (th: Throwable) {
                    logE(TAG, th) { "An exception occurred while checkSystemServer" }
                }
                Transformers.invokeExact(original, frame)
            }, Hooks.EntryPointType.DIRECT)
        }
    }
}
