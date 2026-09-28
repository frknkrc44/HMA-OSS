package org.frknkrc44.hma_oss.xposed

import android.util.Log
import icu.nullptr.hidemyapplist.common.BackendRegistry
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import org.frknkrc44.hma_oss.xposed.service.SystemServerHook

class XposedEntry : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        if (!param.isSystemServer) return
        check(frameworkProperties and XposedInterface.PROP_CAP_SYSTEM != 0L)
        log(Log.INFO, TAG, "Loaded $frameworkName $frameworkVersion, API $apiVersion")
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        if (!BackendRegistry.claim("xposed")) {
            log(Log.WARN, TAG, "Backend already active: ${BackendRegistry.owner()}")
            return
        }
        SystemServerHook.start(this, param.classLoader)
    }

    companion object {
        const val TAG = "HMA-Xposed"
    }
}
