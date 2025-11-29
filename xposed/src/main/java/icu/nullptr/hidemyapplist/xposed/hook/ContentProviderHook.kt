package icu.nullptr.hidemyapplist.xposed.hook

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD

class ContentProviderHook(private val service: HMAService): IFrameworkHook {
    companion object {
        private const val TAG = "ContentProviderHook"
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    override fun load() {
        hooks += findMethod(
            $$"android.content.ContentProvider$Transport"
        ) {
            name == "query"
        }.hookAfter { param ->
            val uri = param.args[1] as Uri?
            val projection = param.args[2] as Array<String>?
            val args = param.args[3] as Bundle?

            if (uri == null) return@hookAfter

            if (uri.authority != "settings") return@hookAfter

            val segments = uri.pathSegments
            if (segments.isEmpty()) return@hookAfter

            val callingApps = Utils4Xposed.getCallingApps(service)
            if (callingApps.isEmpty()) return@hookAfter

            var caller: String? = null

            for (app in callingApps) {
                if (!service.isHookEnabled(app)) continue

                caller = app
                break
            }

            if (caller == null) return@hookAfter

            val uriParts = uri.path + ", " + uri.query + ", " + uri.authority + ", " + uri.pathSegments

            logD(TAG, "@spoofSettings QUERY in ${callingApps.contentToString()}: ($uriParts), ${projection?.contentToString()}, $args")

            val database = segments[0]
            val isListCmd = segments.size < 2

            if (!isListCmd) {
                val name = segments[1]

                logD(TAG, "@spoofSettings QUERY received caller: $caller, database: $database, name: $name")

                val replacement = service.getSpoofedSetting(caller, name, database)
                if (replacement != null) {
                    logD(TAG, "@spoofSettings QUERY $name in $database replaced for $caller")
                    param.result = MatrixCursor(arrayOf("name", "value"), 1).apply {
                        addRow(arrayOf(replacement.name, replacement.value))
                    }
                    service.filterCount++
                }
            } else {
                logD(TAG, "@spoofSettings LIST_QUERY received caller: $caller, database: $database")

                val result = param.result as Cursor? ?: return@hookAfter
                val cache = mutableMapOf<String, String?>()
                val keyIdx = result.getColumnIndex("name")
                val valIdx = result.getColumnIndex("value")

                while (result.moveToNext()) {
                    val name = result.getString(keyIdx)
                    val replacement = service.getSpoofedSetting(caller, name, database)
                    cache[name] = if (replacement != null) {
                        logD(TAG, "@spoofSettings QUERY $name in $database replaced for $caller")

                        replacement.value
                    } else {
                        result.getString(valIdx)
                    }
                }

                val items = mutableListOf("name", "value")
                if (valIdx < keyIdx) items.reverse()

                param.result = MatrixCursor(items.toTypedArray(), cache.size).apply {
                    for (entry in cache.entries) {
                        if (valIdx < keyIdx) addRow(arrayOf(entry.value, entry.key))
                        else addRow(arrayOf(entry.key, entry.value))
                    }
                }
            }
        }

        // Credit: https://github.com/Nitsuya/DoNotTryAccessibility/blob/main/app/src/main/java/io/github/nitsuya/donottryaccessibility/hook/AndroidFrameworkHooker.kt
        hooks += findMethod(
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

                logD(TAG, "@spoofSettings CALL received caller: $caller, method: $method, name: $name")

                when (method) {
                    "GET_global", "GET_secure", "GET_system" -> {
                        val database = method.substring(method.indexOf('_') + 1)
                        val replacement = service.getSpoofedSetting(caller, name, database)
                        if (replacement != null) {
                            logD(TAG, "@spoofSettings CALL $name in $database replaced for $caller")
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

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
