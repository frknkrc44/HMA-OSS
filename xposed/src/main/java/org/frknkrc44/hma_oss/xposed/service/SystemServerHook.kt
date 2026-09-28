package org.frknkrc44.hma_oss.xposed.service

import android.content.pm.IPackageManager
import android.os.ServiceManager
import android.util.Log
import io.github.libxposed.api.XposedInterface
import org.frknkrc44.hma_oss.xposed.XposedEntry
import kotlin.concurrent.thread

object SystemServerHook {
    lateinit var framework: XposedInterface
        private set
    var classLoader: ClassLoader? = null
        private set

    fun start(api: XposedInterface, loader: ClassLoader) {
        framework = api
        classLoader = loader
        thread(name = "HMA-libxposed-init") {
            try {
                var pms: IPackageManager? = null
                // Bound startup waits; never block the system-server lifecycle callback.
                repeat(240) {
                    if (pms == null) {
                        // Within system_server this is the local PackageManagerService
                        // binder, which implements IPackageManager. Our compile-only
                        // hidden-API stub intentionally does not expose its nested Stub.
                        pms = ServiceManager.getService("package") as? IPackageManager
                        if (pms == null) Thread.sleep(250)
                    }
                }
                val packageManager = checkNotNull(pms) { "Package service unavailable after 60s" }
                var activityReady = false
                repeat(240) {
                    if (!activityReady) {
                        activityReady = ServiceManager.getService("activity") != null
                        if (!activityReady) Thread.sleep(250)
                    }
                }
                check(activityReady) { "Activity service unavailable after 60s" }
                UserService.register(packageManager, ServiceManager.getService("package_native"))
                api.log(Log.INFO, XposedEntry.TAG, "HMA service ready (libxposed API ${api.apiVersion})")
            } catch (error: Throwable) {
                api.log(Log.ERROR, XposedEntry.TAG, "HMA initialization failed", error)
            }
        }
    }
}
