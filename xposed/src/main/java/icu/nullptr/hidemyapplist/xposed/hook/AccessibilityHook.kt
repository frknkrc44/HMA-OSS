package icu.nullptr.hidemyapplist.xposed.hook

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.ParceledListSlice
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

// Big credits: https://github.com/Nitsuya/DoNotTryAccessibility/blob/main/app/src/main/java/io/github/nitsuya/donottryaccessibility/hook/AndroidFrameworkHooker.kt
class AccessibilityHook(private val service: HMAService) : IFrameworkHook {
    companion object {
        private const val TAG = "AccessibilityHook"
        private const val ACCESSIBILITY_SERVICE = "com.android.server.accessibility.AccessibilityManagerService"
    }

    private val hookList = mutableSetOf<XC_MethodHook.Unhook>()

    override fun load() {
        logI(TAG, "Load hook")

        hookList += findMethod(ACCESSIBILITY_SERVICE) {
            name == "getInstalledAccessibilityServiceList"
        }.hookBefore { param -> hookedMethod(param, true) }

        hookList += findMethod(ACCESSIBILITY_SERVICE) {
            name == "getEnabledAccessibilityServiceList"
        }.hookBefore { param -> hookedMethod(param, false) }

        hookList += findMethod(ACCESSIBILITY_SERVICE) {
            name == "addClient"
        }.hookBefore { param ->
            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return@hookBefore

            for (caller in callingApps) {
                if (callerIsSpoofed(caller)) {
                    param.result = 0L
                    service.filterCount++
                    break
                }
            }
        }
    }

    private fun callerIsSpoofed(caller: String) =
        service.getEnabledSettingsPresets(caller).contains(AccessibilityPreset.NAME)

    private fun hookedMethod(param: XC_MethodHook.MethodHookParam, returnParcel: Boolean) {
        try {
            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return

            for (caller in callingApps) {
                if (callerIsSpoofed(caller)) {
                    val returnedList = java.util.ArrayList<AccessibilityServiceInfo>()

                    logD(TAG, "@${param.method.name} returned empty list for $caller")

                    param.result = if (returnParcel) {
                         ParceledListSlice(returnedList)
                    } else {
                        returnedList
                    }

                    service.filterCount++
                    break
                }
            }
        } catch (e: Throwable) {
            logE(TAG, "Fatal error occurred, ignore hooks", e)
        }
    }

    override fun unload() {
        hookList.forEach(XC_MethodHook.Unhook::unhook)
        hookList.clear()
    }
}