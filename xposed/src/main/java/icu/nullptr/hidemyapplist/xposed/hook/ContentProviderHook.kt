package icu.nullptr.hidemyapplist.xposed.hook

import android.content.AttributionSource
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
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
        private const val CONTENT_PROVIDER_TRANSPORT = $$"android.content.ContentProvider$Transport"
        private val NV_PAIR = arrayOf("name", "value")
    }

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        hooks += findMethod(CONTENT_PROVIDER_TRANSPORT) {
            name == "query"
        }.hookAfter { param ->
            val callingApps = getCallingPackages(param)

            val caller = callingApps.firstOrNull { service.isHookEnabled(it) }
            if (caller == null) return@hookAfter

            val uri = param.args[1] as Uri?
            val projection = param.args[2] as Array<String>?
            val args = param.args[3] as Bundle?

            if (uri?.authority != "settings") return@hookAfter

            val segments = uri.pathSegments
            if (segments.isEmpty()) return@hookAfter

            logD(TAG, "@spoofSettings QUERY in ${callingApps.contentToString()}: $uri, ${projection?.contentToString()}, $args")

            val database = segments[0]

            if (segments.size >= 2) {
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

                val columns = mutableMapOf<String, MutableList<String?>>().apply {
                    for (i in 0 ..< result.columnCount) {
                        put(result.getColumnName(i), mutableListOf())
                    }
                }

                logD(TAG, "@spoofSetting LIST_QUERY columns: ${columns.keys}")

                val keyColumn = columns["name"]
                val valueColumn = columns["value"]

                if (keyColumn == null || valueColumn == null) {
                    logD(TAG, "@spoofSettings LIST_QUERY invalid query: $caller ($keyColumn, $valueColumn)")
                    return@hookAfter
                }

                while (result.moveToNext()) {
                    val name = result.getString(columns.keys.indexOf("name"))
                    keyColumn.add(name)

                    val replacement = service.getSpoofedSetting(caller, name, database)
                    val value = if (replacement != null) {
                        logD(TAG, "@spoofSettings QUERY $name in $database replaced for $caller")

                        replacement.value
                    } else {
                        result.getString(columns.keys.indexOf("value"))
                    }

                    valueColumn.add(value)

                    if (columns.keys.size > 2) {
                        for (otherCol in columns.keys.filter { it !in NV_PAIR }) {
                            val other = result.getString(columns.keys.indexOf(otherCol))

                            columns[otherCol]!!.add(other)
                        }
                    }
                }

                param.result = MatrixCursor(columns.keys.toTypedArray(), columns.size).apply {
                    val size = columns.values.first().size
                    for (i in 0 ..< size) {
                        val innerList = mutableListOf<String?>()

                        columns.values.forEach { colVal ->
                            innerList.add(colVal[i])
                        }

                        addRow(innerList)
                    }
                }
            }
        }

        // Credit: https://github.com/Nitsuya/DoNotTryAccessibility/blob/main/app/src/main/java/io/github/nitsuya/donottryaccessibility/hook/AndroidFrameworkHooker.kt
        hooks += findMethod(CONTENT_PROVIDER_TRANSPORT) {
            name == "call"
        }.hookBefore { param ->
            val callingApps = getCallingPackages(param)
            val caller = callingApps.firstOrNull { service.isHookEnabled(it) }
            if (caller == null) return@hookBefore

            val method = param.args[2] as String?
            val name = param.args[3] as String?

            logD(TAG, "@spoofSettings CALL received caller: ${callingApps.contentToString()}, method: $method, name: $name")

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

    private fun getCallingPackages(param: XC_MethodHook.MethodHookParam) = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attrSource = param.args.first() as AttributionSource
            arrayOf(attrSource.packageName)
        } else {
            arrayOf(param.args.first() as String)
        }
    } catch (_: Throwable) {
        Utils4Xposed.getCallingApps(service)
    }

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
