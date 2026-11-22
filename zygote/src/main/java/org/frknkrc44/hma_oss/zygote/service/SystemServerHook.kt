package org.frknkrc44.hma_oss.zygote.service

import android.annotation.SuppressLint
import android.content.pm.IPackageManager
import android.os.Looper
import com.v7878.r8.annotations.DoNotShrink
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_NATIVE_SERVICE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.SYSTEM_SERVER_CLASS
import kotlin.concurrent.thread

@SuppressLint("PrivateApi")
object SystemServerHook {
    private const val TAG = "SystemServerHook"

    var classLoader: ClassLoader? = null

    @Throws(Throwable::class)
    fun onSystemServer(loader: ClassLoader?) {
        assert(loader != null) { "Class loader is null, aborting!" }

        logV(TAG) { "Class loader found: $loader" }

        classLoader = loader

        val pms = waitForService(PACKAGE_MANAGER_SERVICE) as IPackageManager
        val pmn = waitForService(PACKAGE_MANAGER_NATIVE_SERVICE)
        logD(TAG) { "Got pms: $pms, $pmn" }

        try {
            UserService.register(pms, pmn)
            logI(TAG) { "User service started" }
        } catch (cause: Throwable) {
            logE(TAG, cause) { "System service crashed" }
        }
    }

    @DoNotShrink
    @Throws(Throwable::class)
    @JvmStatic
    fun init() {
        logI(TAG) { "Trying to invoke universal mode" }

        thread {
            while (true) {
                try {
                    Looper.getMainLooper()?.thread?.contextClassLoader?.apply {
                        loadClass(SYSTEM_SERVER_CLASS)
                        onSystemServer(this)
                        break
                    }

                    Thread.sleep(100)
                } catch (_: Throwable) {}
            }
        }
    }
}
