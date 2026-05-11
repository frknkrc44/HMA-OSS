package org.frknkrc44.hma_oss.zygote.service

import android.annotation.SuppressLint
import android.content.pm.IPackageManager
import android.os.Build
import com.v7878.r8.annotations.DoNotShrink
import com.v7878.unsafe.Reflection.getDeclaredMethod
import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.Hooks
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.callStaticMethod
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.waitForService
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
        assert(loader != null) { "Class loader is null, aborting!" }

        logV(TAG) { "Class loader found: $loader" }

        classLoader = loader

        if (!initialized) {
            initialized = true

            thread {
                val pms = waitForService("package") as IPackageManager
                val pmn = waitForService("package_native")
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

    @DoNotShrink
    @Throws(Throwable::class)
    @JvmStatic
    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            logI(TAG) { "Trying to invoke 12+ mode" }

            runCatching {
                val loader = callStaticMethod(
                    Class.forName(ZYGOTE_INIT),
                    "getOrCreateSystemServerClassLoader"
                )

                onSystemServer(loader as? ClassLoader)
            }.onSuccess {
                return
            }.onFailure {
                logE(TAG, it) { "An exception occurred while trying 12+ mode" }
                // falls back to 11- mode
            }
        }

        logI(TAG) { "Trying to invoke 11- mode" }

        val method = getDeclaredMethod(
            Class.forName(RUNTIME_INIT), "findStaticMain",
            String::class.java, Array<String>::class.java, ClassLoader::class.java
        )

        Hooks.hook(method, Hooks.EntryPointType.CURRENT, { original, frame ->
            try {
                val accessor = frame.accessor()
                if (SYSTEM_SERVER == accessor.getReference(0)) {
                    val loader: ClassLoader? = accessor.getReference(2)
                    onSystemServer(loader)
                }
            } catch (th: Throwable) {
                logE(TAG, th) { "An exception occurred while checkSystemServer" }
            }
            Transformers.invokeExact(original, frame)
        }, Hooks.EntryPointType.DIRECT)
    }
}
