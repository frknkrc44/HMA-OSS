package icu.nullptr.hidemyapplist.xposed.hook

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.ParceledListSlice
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Logcat.logD
import icu.nullptr.hidemyapplist.xposed.Logcat.logI
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.XposedConstants.ACCESSIBILITY_SERVICE_CLASS
import java.lang.reflect.Method

// Big credits: https://github.com/Nitsuya/DoNotTryAccessibility/blob/main/app/src/main/java/io/github/nitsuya/donottryaccessibility/hook/AndroidFrameworkHooker.kt
class AccessibilityHook(private val service: HMAService) : IFrameworkHook {
    companion object {
        private const val TAG = "AccessibilityHook"
    }

    private val hookList = mutableSetOf<XC_MethodHook.Unhook>()

    override fun load() {
        logI(TAG) { "Load hook" }

        hookList += findMethod(ACCESSIBILITY_SERVICE_CLASS) {
            name == "getEnabledAccessibilityServiceList"
        }.hookBefore { param ->
            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return@hookBefore

            val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
            if (caller != null) {
                val returnedList = java.util.ArrayList<AccessibilityServiceInfo>()

                logD(TAG) { "@${param.method.name} returned empty list for ${callingApps.contentToString()}" }

                val returnType = (param.method as Method).returnType
                param.result = if ("Parcel" in returnType.javaClass.simpleName) {
                    ParceledListSlice(returnedList)
                } else {
                    returnedList
                }
            }
        }

        hookList += findMethod(ACCESSIBILITY_SERVICE_CLASS) {
            name == "addClient"
        }.hookBefore { param ->
            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return@hookBefore

            val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
            if (caller != null) {
                param.result = 0L
            }
        }
    }

    private fun callerIsSpoofed(caller: String) =
        service.getEnabledSettingsPresets(caller).contains(AccessibilityPreset.NAME)

    override fun unload() {
        hookList.forEach(XC_MethodHook.Unhook::unhook)
        hookList.clear()
    }
}
