package org.frknkrc44.hma_oss.zygote.hook

import android.content.AttributionSource
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstWithType
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.service.HookParam
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONTENT_PROVIDER_TRANSPORT_CLASS

class ContentProviderHook(private val service: HMAService): IFrameworkHook {
    override val TAG = "ContentProviderHook"

    companion object {
        private val NV_PAIR = arrayOf("name", "value")
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        BulkHooker.instance.apply {
            hookAfter(
                CONTENT_PROVIDER_TRANSPORT_CLASS,
                "query",
            ) { param ->
                val callingApps = getCallingPackages(param)

                val caller = callingApps.firstOrNull { service.isAnySettingsReplacementsEnabled(it) }
                if (caller == null) return@hookAfter

                val uriIdx = param.args.indexOfFirst { it is Uri }
                val uri = param.args[uriIdx] as Uri

                if (uri.authority != "settings") return@hookAfter

                val segments = uri.pathSegments
                if (segments.isEmpty()) return@hookAfter

                logD(TAG) {
                    val projection = param.args[uriIdx + 1] as Array<String>?
                    val args = param.args[uriIdx + 2] as Bundle?

                    "@spoofSettings QUERY in ${callingApps.contentToString()}: $uri, ${projection?.contentToString()}, $args"
                }

                val database = segments[0]

                if (segments.size >= 2) {
                    val name = segments[1]

                    logD(TAG) { "@spoofSettings QUERY received caller: $caller, database: $database, name: $name" }

                    val replacement = service.getSpoofedSetting(caller, name, database)
                    if (replacement != null) {
                        logD(TAG) { "@spoofSettings QUERY $name in $database replaced for $caller" }
                        param.result = MatrixCursor(arrayOf("name", "value"), 1).apply {
                            addRow(arrayOf(replacement.name, replacement.value))
                        }

                        service.increaseSettingsFilterCount(caller)
                    }
                } else {
                    logD(TAG) { "@spoofSettings LIST_QUERY received caller: $caller, database: $database" }

                    val result = param.result as? Cursor? ?: return@hookAfter

                    val columns = mutableMapOf<String, MutableList<String?>>().apply {
                        for (i in 0 ..< result.columnCount) {
                            put(result.getColumnName(i), mutableListOf())
                        }
                    }

                    logD(TAG) { "@spoofSetting LIST_QUERY columns: ${columns.keys}" }

                    val keyColumn = columns["name"]
                    val valueColumn = columns["value"]

                    if (keyColumn == null || valueColumn == null) {
                        logD(TAG) { "@spoofSettings LIST_QUERY invalid query: $caller ($keyColumn, $valueColumn)" }
                        return@hookAfter
                    }

                    val allSpoofed = service.getAllSpoofedSettings(caller, database) ?: return@hookAfter
                    if (allSpoofed.isEmpty()) return@hookAfter

                    var filteredEntryCount = 0

                    while (result.moveToNext()) {
                        val name = result.getString(columns.keys.indexOf("name"))
                        keyColumn.add(name)

                        val replacement = allSpoofed.firstOrNull { it.name == name }
                        val value = if (replacement != null) {
                            logD(TAG) { "@spoofSettings QUERY $name in $database replaced for $caller" }

                            filteredEntryCount++

                            replacement.value
                        } else {
                            result.getString(columns.keys.indexOf("value"))
                        }

                        valueColumn.add(value)

                        if (columns.size > 2) {
                            for (otherCol in columns.keys.filter { it !in NV_PAIR }) {
                                val other = result.getString(columns.keys.indexOf(otherCol))

                                columns[otherCol]!!.add(other)
                            }
                        }
                    }

                    // we cannot provide info about other columns for non-existent items
                    if (columns.size == 2) {
                        allSpoofed.forEach { item ->
                            if (item.name !in keyColumn) {
                                val putIndex = keyColumn.indexOfFirst { key ->
                                    key?.let { it > item.name } ?: false
                                }

                                if (putIndex < 0) {
                                    keyColumn.add(item.name)
                                    valueColumn.add(item.value)
                                } else {
                                    keyColumn.add(putIndex, item.name)
                                    valueColumn.add(putIndex, item.value)
                                }
                            }
                        }
                    }

                    service.increaseSettingsFilterCount(caller, filteredEntryCount)

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

            hookBefore(
                CONTENT_PROVIDER_TRANSPORT_CLASS,
                "call",
            ) { param ->
                val callingApps = getCallingPackages(param)
                val caller = callingApps.firstOrNull { service.isAnySettingsReplacementsEnabled(it) }
                if (caller == null) return@hookBefore

                val nameIdx = param.args.indexOfLast { it is String }
                val name = param.args[nameIdx] as String?
                val method = param.args[nameIdx - 1] as String?

                logD(TAG) { "@spoofSettings CALL received caller: ${callingApps.contentToString()}, method: $method, name: $name" }

                when (method) {
                    "GET_global", "GET_secure", "GET_system" -> {
                        val database = method.substring(method.indexOf('_') + 1)
                        val replacement = service.getSpoofedSetting(caller, name, database)
                        if (replacement != null) {
                            logD(TAG) { "@spoofSettings CALL $name in $database replaced for $caller" }
                            param.result = Bundle().apply {
                                putString(Settings.NameValueTable.VALUE, replacement.value)
                                putInt("_generation_index", -1)
                            }

                            service.increaseSettingsFilterCount(caller)
                        }
                    }
                }
            }
        }
    }

    private fun getCallingPackages(param: HookParam): Array<String> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attrSource = param.args.firstWithType<AttributionSource>()
            arrayOf(attrSource.packageName!!)
        } else {
            arrayOf(param.args.firstWithType<String>())
        }
    } catch (_: Throwable) {
        ServiceUtils.getCallingApps(service)
    }
}
