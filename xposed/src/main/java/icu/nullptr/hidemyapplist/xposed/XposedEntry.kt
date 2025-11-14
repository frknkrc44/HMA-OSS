package icu.nullptr.hidemyapplist.xposed

import android.content.pm.IPackageManager
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.concurrent.thread

private const val TAG = "HMA-XposedEntry"

@Suppress("unused")
class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {
    var targetsLeft = mutableListOf("package", "package_native")
    var targetStorage = mutableMapOf<String, Any?>()

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android") {
            EzXHelperInit.initHandleLoadPackage(lpparam)
            logI(TAG, "Hook entry")

            var serviceManagerHook: XC_MethodHook.Unhook? = null
            serviceManagerHook = findMethod("android.os.ServiceManager") {
                name == "addService"
            }.hookBefore { param ->
                val name = param.args[0] as String
                if (targetsLeft.contains(name)) {
                    when (name) {
                        "package", "package_native" -> {
                            targetStorage[name] = param.args[1]
                            targetsLeft.remove(name)
                        }
                        else -> {
                            // skip if there is no package_native available
                            if (targetStorage.containsKey("package")) {
                                targetsLeft.remove("package_native")
                            }
                        }
                    }
                }

                if (targetsLeft.isEmpty()) {
                    serviceManagerHook?.unhook()
                    val pms = targetStorage["package"] as IPackageManager
                    val pmn = targetStorage["package_native"]
                    logD(TAG, "Got pms: $pms, $pmn")
                    thread {
                        runCatching {
                            UserService.register(pms, pmn)
                            targetStorage.clear()
                            logI(TAG, "User service started")
                        }.onFailure {
                            logE(TAG, "System service crashed", it)
                        }
                    }
                }
            }
        }
    }
}
