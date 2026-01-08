package icu.nullptr.hidemyapplist.xposed.hook

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PACKAGE_MANAGER_SERVICE_CLASS

class PmsPackageEventsHook(private val service: HMAService) : IFrameworkHook {
    private var hook: XC_MethodHook.Unhook? = null

    override fun load() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                hook = findMethod("com.android.server.pm.BroadcastHelper") {
                    name == "sendPackageBroadcastAndNotify"
                }.hookBefore { param ->
                    service.handlePackageEvent(
                        param.args[0] as String?,
                        param.args[1] as String?,
                        param.args[2] as Bundle?,
                    )
                }
            } catch (_: Throwable) {
                hook = findMethod("com.android.internal.content.PackageMonitor") {
                    name == "onReceive"
                }.hookBefore { param ->
                    val intent = param.args[1] as Intent? ?: return@hookBefore

                    service.handlePackageEvent(
                        intent.action,
                        intent.data?.encodedSchemeSpecificPart,
                        intent.extras,
                    )
                }
            }
        } else {
            hook = findMethod(PACKAGE_MANAGER_SERVICE_CLASS) {
                name == "sendPackageBroadcast"
            }.hookBefore { param ->
                service.handlePackageEvent(
                    param.args[0] as String?,
                    param.args[1] as String?,
                    param.args[2] as Bundle?,
                )
            }
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }
}
