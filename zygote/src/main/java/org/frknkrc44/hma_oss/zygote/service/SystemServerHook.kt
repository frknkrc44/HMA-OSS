package org.frknkrc44.hma_oss.zygote.service

import android.annotation.SuppressLint
import android.content.pm.IPackageManager
import android.os.Looper
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

    @JvmStatic
    fun init() {
        logI(TAG) { "Trying to invoke universal mode" }

        thread {
            while (classLoader == null) {
                try {
                    Looper.getMainLooper()?.thread?.contextClassLoader?.apply {
                        loadClass(SYSTEM_SERVER_CLASS)

                        classLoader = this
                        logV(TAG) { "Class loader found: $classLoader" }

                        break
                    }

                    Thread.sleep(100)
                } catch (_: Throwable) {}
            }

            logI(TAG) { "Eureka!" }

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
    }
}
