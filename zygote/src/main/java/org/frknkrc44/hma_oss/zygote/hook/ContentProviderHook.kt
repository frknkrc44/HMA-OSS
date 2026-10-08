package org.frknkrc44.hma_oss.zygote.hook

import android.content.AttributionSource
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstWithType
import org.frknkrc44.hma_oss.zygote.util.ContentProviderUtils.getOverriddenDatabaseName
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONTENT_PROVIDER_TRANSPORT_CLASS

class ContentProviderHook : IFrameworkHook {
    override val TAG = "ContentProviderHook"

    companion object {
        private val NV_PAIR = arrayOf("name", "value")
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        hooker.apply {
            hookAfter(
                CONTENT_PROVIDER_TRANSPORT_CLASS,
                "query",
            ) { _, frame, returnValue ->
                val args = dumpArgs(frame, true)
                val callingApps = getCallingPackages(args)

                val caller = callingApps.firstOrNull { service.isAnySettingsReplacementsEnabled(it) }
                if (caller == null) return@hookAfter

                val uriIdx = args.indexOfFirst { it is Uri }
                val uri = args[uriIdx] as Uri

                if (uri.authority != "settings") return@hookAfter

                val segments = uri.pathSegments
                if (segments.isEmpty()) return@hookAfter

                val projection = args[uriIdx + 1] as? Array<String>
                val projectionArgs = args[uriIdx + 2] as? Bundle

                logD(TAG, null) {
                    "@spoofSettings QUERY in ${callingApps.contentToString()}: $uri, ${projection?.contentToString()}, $projectionArgs"
                }

                var database = segments[0]

                if (segments.size >= 2) {
                    val name = segments[1]

                    logD(TAG, null) { "@spoofSettings QUERY received caller: $caller, database: $database, name: $name, args: $projectionArgs" }

                    database = getOverriddenDatabaseName(database, name)

                    val replacement = service.getSpoofedSetting(caller, name, database)
                    if (replacement != null) {
                        val columnNames = projection ?: arrayOf("name", "value")
                        val nameInColumns = "name" in columnNames
                        val valueInColumns = "value" in columnNames

                        val returnedArray = when {
                            nameInColumns && valueInColumns -> arrayOf(replacement.name, replacement.value)
                            valueInColumns -> arrayOf(replacement.value)
                            nameInColumns -> arrayOf(replacement.name)
                            else -> return@hookAfter
                        }

                        logD(TAG, null) { "@spoofSettings QUERY $name in $database replaced for $caller" }
                        returnValue.result = MatrixCursor(columnNames, 1).apply {
                            addRow(returnedArray)
                        }

                        service.increaseSettingsFilterCount(caller)
                    }
                } else {
                    logD(TAG, null) { "@spoofSettings LIST_QUERY received caller: $caller, database: $database" }

                    val result = returnValue.result as? Cursor ?: return@hookAfter

                    val columns = mutableMapOf<String, MutableList<String?>>().apply {
                        for (i in 0 ..< result.columnCount) {
                            put(result.getColumnName(i), mutableListOf())
                        }
                    }

                    logD(TAG, null) { "@spoofSetting LIST_QUERY columns: ${columns.keys}" }

                    val keyColumn = columns["name"]
                    val valueColumn = columns["value"]

                    if (keyColumn == null || valueColumn == null) {
                        logD(TAG, null) { "@spoofSettings LIST_QUERY invalid query: $caller ($keyColumn, $valueColumn)" }
                        return@hookAfter
                    }

                    var filteredEntryCount = 0

                    while (result.moveToNext()) {
                        val name = result.getString(columns.keys.indexOf("name"))
                        keyColumn.add(name)

                        val dbName = getOverriddenDatabaseName(database, name)
                        val replacement = service.getSpoofedSetting(caller, name, dbName)
                        val value = if (replacement != null) {
                            logD(TAG, null) { "@spoofSettings QUERY $name in $database replaced for $caller" }

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

                    service.increaseSettingsFilterCount(caller, filteredEntryCount)

                    returnValue.result = MatrixCursor(columns.keys.toTypedArray(), columns.size).apply {
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
            ) { _, frame, returnValue ->
                val args = dumpArgs(frame, true)
                val callingApps = getCallingPackages(args)
                val caller = callingApps.firstOrNull { service.isAnySettingsReplacementsEnabled(it) }
                if (caller == null) return@hookBefore

                val nameIdx = args.indexOfLast { it is String }
                val name = args[nameIdx] as? String
                val method = args[nameIdx - 1] as? String

                logD(TAG, null) { "@spoofSettings CALL received caller: ${callingApps.contentToString()}, method: $method, name: $name" }

                when (method) {
                    "GET_global", "GET_secure", "GET_system" -> {
                        val database = method.substring(method.indexOf('_') + 1)
                        val replacement = service.getSpoofedSetting(caller, name, database)
                        if (replacement != null) {
                            logD(TAG, null) { "@spoofSettings CALL $name in $database replaced for $caller" }
                            returnValue.result = Bundle().apply {
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

    private fun getCallingPackages(args: Array<Any>): Array<String> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attrSource = args.firstWithType<AttributionSource>()
            arrayOf(attrSource.packageName!!)
        } else {
            arrayOf(args.firstWithType<String>())
        }
    } catch (_: Throwable) {
        ServiceUtils.getCallingApps(pms)
    }
}
