package icu.nullptr.hidemyapplist.xposed.hook

import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodSubtype
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.common.settings_presets.InputMethodPreset
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import java.util.Collections

class ImmHook(private val service: HMAService) : IFrameworkHook {
    companion object {
        private const val TAG = "ImmHook"
        private const val IMM_SERVICE = "com.android.server.inputmethod.InputMethodManagerService"
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    // TODO: Find a method to get settings activity
    fun getFakeInputMethodInfo(packageName: String): InputMethodInfo {
        val defaultInputMethod = service.getSpoofedSetting(
            packageName,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            Constants.SETTINGS_SECURE,
        )

        if (defaultInputMethod?.value != null) {
            try {
                val component = ComponentName.unflattenFromString(defaultInputMethod.value!!)!!
                logD(TAG, "Package component: \"$component\"")

                val pkgManager = Utils4Xposed.getPackageManager()
                val kbdPackage = Utils.binderLocalScope {
                    pkgManager.getApplicationInfo(component.packageName, 0)
                }

                return InputMethodInfo(
                    component.packageName,
                    component.className,
                    kbdPackage.loadLabel(pkgManager),
                    null,
                )
            } catch (e: Throwable) {
                logE(TAG, e.message ?: "", e)
            }
        }

        return InputMethodInfo(
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin.LatinIME",
            "Gboard",
            null,
        )
    }

    override fun load() {
        logI(TAG, "Load hook")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            findMethodOrNull(IMM_SERVICE) {
                name == "getCurrentInputMethodInfoAsUser"
            }?.hookBefore { param ->
                val callingApps = Utils4Xposed.getCallingApps(service)

                for (caller in callingApps) {
                    if (callerIsSpoofed(caller)) {
                        logD(TAG, "@${param.method.name} spoofed input method for $caller")

                        param.result = getFakeInputMethodInfo(caller)
                        service.filterCount++
                        break
                    }
                }
            }?.let {
                logD(TAG, "@${it.hookedMethod.name} is hooked!")
                hooks += it
            }
        }

        (findMethodOrNull(IMM_SERVICE) {
            name == "getInputMethodListInternal"
        } ?: findMethodOrNull(IMM_SERVICE) {
            name == "getInputMethodList" && returnType.simpleName != "InputMethodInfoSafeList"
        })?.hookBefore { param ->
            listHook(param)
        }?.let {
            logD(TAG, "@${it.hookedMethod.name} is hooked!")
            hooks += it
        }

        (findMethodOrNull(IMM_SERVICE) {
            name == "getEnabledInputMethodListInternal"
        } ?: findMethodOrNull(IMM_SERVICE) {
            name == "getEnabledInputMethodList" && returnType.simpleName != "InputMethodInfoSafeList"
        })?.hookBefore { param ->
            listHook(param)
        }?.let {
            logD(TAG, "@${it.hookedMethod.name} is hooked!")
            hooks += it
        }

        findMethodOrNull(IMM_SERVICE) {
            name == "getCurrentInputMethodSubtype"
        }?.hookBefore { param ->
            subtypeHook(param)
        }?.let {
            logD(TAG, "@${it.hookedMethod.name} is hooked!")
            hooks += it
        }

        findMethodOrNull(IMM_SERVICE) {
            name == "getLastInputMethodSubtype"
        }?.hookBefore { param ->
            subtypeHook(param)
        }?.let {
            logD(TAG, "@${it.hookedMethod.name} is hooked!")
            hooks += it
        }

        (findMethodOrNull(IMM_SERVICE) {
            name == "getEnabledInputMethodSubtypeListInternal"
        } ?: findMethodOrNull(IMM_SERVICE) {
            name == "getEnabledInputMethodSubtypeList"
        })?.hookBefore { param ->
            subtypeListHook(param)
        }?.let {
            logD(TAG, "@${it.hookedMethod.name} is hooked!")
            hooks += it
        }
    }

    private fun listHook(param: XC_MethodHook.MethodHookParam) {
        val callingApps = if (param.method.name.endsWith("Internal")) {
            val callingUid = param.args.last() as Int
            Utils4Xposed.getCallingApps(service, callingUid)
        } else {
            Utils4Xposed.getCallingApps(service)
        }

        for (caller in callingApps) {
            if (callerIsSpoofed(caller)) {
                logD(TAG, "@${param.method.name} spoofed input method for $caller")

                param.result = listOf(getFakeInputMethodInfo(caller))
                service.filterCount++
                break
            }
        }
    }

    private fun subtypeHook(param: XC_MethodHook.MethodHookParam) {
        val callingApps = Utils4Xposed.getCallingApps(service)

        for (caller in callingApps) {
            if (callerIsSpoofed(caller)) {
                logD(TAG, "@${param.method.name} spoofed input method subtype for $caller")

                // TODO: Find a method to get exact value for spoofed input method
                param.result = null
                service.filterCount++
                break
            }
        }
    }

    private fun subtypeListHook(param: XC_MethodHook.MethodHookParam) {
        val callingApps = Utils4Xposed.getCallingApps(service)

        for (caller in callingApps) {
            if (callerIsSpoofed(caller)) {
                logD(TAG, "@${param.method.name} spoofed input method subtype for $caller")

                // TODO: Find a method to get exact list for spoofed input method
                param.result = Collections.emptyList<InputMethodSubtype>()
                service.filterCount++
                break
            }
        }
    }

    private fun callerIsSpoofed(caller: String) =
        service.getEnabledSettingsPresets(caller).contains(InputMethodPreset.NAME)

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}