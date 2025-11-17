package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Bundle
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.SettingsPresets
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logI

class ContentProviderHook(private val service: HMAService): IFrameworkHook {
    companion object {
        private const val TAG = "ContentProviderHook"
    }

    private var hook: XC_MethodHook.Unhook? = null

    // Credit: https://github.com/Nitsuya/DoNotTryAccessibility/blob/main/app/src/main/java/io/github/nitsuya/donottryaccessibility/hook/AndroidFrameworkHooker.kt
    override fun load() {
        hook = findMethod(
            $$"android.content.ContentProvider$Transport"
        ) {
            name == "call"
        }.hookBefore { param ->
            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return@hookBefore

            val method = param.args[2] as String?
            val name = param.args[3] as String?

            for (caller in callingApps) {
                if (!service.isHookEnabled(caller)) continue

                logD(TAG, "@spoofSettings received caller: $caller, method: $method, name: $name")

                when (method) {
                    "GET_global", "GET_secure", "GET_system" -> {
                        val database = method.substring(method.indexOf('_') + 1)
                        val replacement = getSpoofedSetting(caller, name, database)
                        if (replacement != null) {
                            logI(TAG, "@spoofSettings $name in $database replaced for $caller")
                            param.result = Bundle().apply {
                                putString(Settings.NameValueTable.VALUE, replacement.value)
                                putInt("_generation_index", -1)
                            }
                            service.filterCount++
                        }
                    }
                }
            }
        }
    }

    fun getSpoofedSetting(caller: String?, name: String?, database: String): ReplacementItem? {
        if (caller == null || name == null) return null

        val templates = service.getEnabledSettingsTemplates(caller)
        val replacement = service.config.settingsTemplates.firstNotNullOfOrNull { (key, value) ->
            if (key in templates) value.settingsList.firstOrNull { it.name == name } else null
        }
        if (replacement != null) return replacement

        val presets = service.getEnabledSettingsPresets(caller)
        if (presets.isNotEmpty()) {
            for (presetName in presets) {
                val preset = SettingsPresets.instance.getPresetByName(presetName)
                val replacement = preset?.getSpoofedValue(name)
                if (replacement?.database == database) return replacement
            }
        }

        return null
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }
}