package icu.nullptr.hidemyapplist.xposed.hook

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.getStaticIntField
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.XposedConstants.ACTIVITY_STACK_SUPERVISOR_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.ACTIVITY_STARTER_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.ACTIVITY_TASK_SUPERVISOR_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.COMPUTER_ENGINE_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PACKAGE_MANAGER_SERVICE_CLASS
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

class ActivityHook(private val service: HMAService) : IFrameworkHook {
    companion object {
        private const val TAG = "ActivityHook"
        private val fakeReturnCode by lazy {
            getStaticIntField(
                findClass(
                    "android.app.ActivityManager",
                    InitFields.ezXClassLoader
                ),
                "START_CLASS_NOT_FOUND"
            )
        }
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    override fun load() {
        logI(TAG, "Load hook")

        hooks += findMethod(ACTIVITY_STARTER_CLASS) {
            name == "execute"
        }.hookBefore { param ->
            runCatching {
                val request = getObjectField(param.thisObject, "mRequest")
                val caller = getObjectField(request, "callingPackage") as String?
                val intent = getObjectField(request, "intent") as Intent?
                val targetApp = intent?.component?.packageName

                if (service.shouldHideActivityLaunch(caller, targetApp)) {
                    logD(
                        TAG,
                        "@executeRequest: insecure query from $caller, target: ${intent?.component}"
                    )
                    param.result = fakeReturnCode
                    service.filterCount++
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, ignore hook\n", it)
                // unload()
            }
        }

        findMethodOrNull(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ACTIVITY_TASK_SUPERVISOR_CLASS
        } else {
            ACTIVITY_STACK_SUPERVISOR_CLASS
        }) {
            name == "checkStartAnyActivityPermission"
        }?.hookAfter { param ->
            var throwable = param.throwable

            while (throwable != null) {
                val newTrace = throwable.stackTrace.filter { item ->
                    !Utils.containsMultiple(
                        item.className,
                        "HookBridge",
                        "LSPHooker",
                        "LSPosed",
                    )
                }

                if (newTrace.size != throwable.stackTrace.size) {
                    throwable.stackTrace = newTrace.toTypedArray()
                    service.filterCount++
                    logD(TAG, "@checkStartAnyActivityPermission: ${throwable.stackTrace.size - newTrace.size} remnants cleared!")
                }

                throwable = throwable.cause
            }
        }?.let {
            hooks += it
            logD(TAG, "Loaded ${it.hookedMethod.name} hook from ${it.hookedMethod.declaringClass}!")
        }

        try {
            (findMethodOrNull(COMPUTER_ENGINE_CLASS) {
                name == "applyPostResolutionFilter"
            } ?: findMethodOrNull(PACKAGE_MANAGER_SERVICE_CLASS) {
                name == "applyPostResolutionFilter"
            })?.hookBefore { param ->
                @Suppress("UNCHECKED_CAST") // I know what I do
                val list = param.args.first() as List<ResolveInfo>?
                if (list.isNullOrEmpty()) return@hookBefore

                val callingUid = param.args.first { it is Int } as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                for (caller in callingApps) {
                    if (!service.isHookEnabled(caller)) continue

                    // logD(TAG, "@${param.method.name}: $caller requested a resolve info")

                    val filteredList = list.filter { resolveInfo ->
                        val targetApp = Utils.getPackageNameFromResolveInfo(resolveInfo)

                        // logD(TAG, "@${param.method.name}: Checking $targetApp for $caller")

                        (!service.shouldHideActivityLaunch(caller, targetApp)).apply {
                            if (!this) {
                                logD(TAG, "@${param.method.name}: Filtered $targetApp from $caller")
                            }
                        }
                    }

                    if (filteredList.size != list.size) {
                        param.args[0] = filteredList.toList()

                        service.filterCount++
                    }
                }
            }?.let {
                hooks += it
            }
        } catch (e: Throwable) {
            logE(TAG, e.toString(), e)
        }
    }

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
