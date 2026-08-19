package org.frknkrc44.hma_oss.zygote.util

import android.os.SystemProperties
import android.util.Log
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("SpellCheckingInspection")
object Logcat {
    private val logdReady: Boolean by lazy { SystemProperties.get("init.svc.logd") == "running" }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun logV(tag: String, cause: Throwable? = null, msg: () -> String) = logWithLevel(Log.VERBOSE, tag, cause, msg)

    fun logD(tag: String, cause: Throwable? = null, msg: () -> String) = logWithLevel(Log.DEBUG, tag, cause, msg)

    fun logI(tag: String, cause: Throwable? = null, msg: () -> String) = logWithLevel(Log.INFO, tag, cause, msg)

    fun logW(tag: String, cause: Throwable? = null, msg: () -> String) = logWithLevel(Log.WARN, tag, cause, msg)

    fun logE(tag: String, cause: Throwable? = null, msg: () -> String) = logWithLevel(Log.ERROR, tag, cause, msg)

    @JvmStatic
    fun logILegacy(tag: String, msg: String) = logI(tag) { msg }

    @JvmStatic
    fun logELegacy(tag: String, msg: String, cause: Throwable?) = logE(tag, cause) { msg }

    fun logWithLevel(level: Int, tag: String, cause: Throwable? = null, msg: () -> String) {
        if (level != Log.ERROR && service?.config?.errorOnlyLog == true) return
        if (level <= Log.DEBUG && service?.config?.detailLog == false) return
        if (level == Log.VERBOSE && !BuildConfig.DEBUG) return

        val parsedMsg = parseLog(level, tag, msg(), cause)

        // add into our log
        service?.apply {
            executor.execute {
                addLog(parsedMsg)
            }
        }

        // add into logcat if available
        println(parsedMsg)
    }

    private fun parseLog(level: Int, tag: String, msg: String, cause: Throwable? = null) = buildString {
        val levelStr = when (level) {
            Log.VERBOSE -> "VERBS"
            Log.DEBUG   -> "DEBUG"
            Log.INFO    -> " INFO"
            Log.WARN    -> " WARN"
            Log.ERROR   -> "ERROR"
            else        -> "?WTF?"
        }
        val date = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        append("[$levelStr] <$date> ($tag) $msg")
        if (!endsWith('\n')) append('\n')
        if (cause != null) append(Log.getStackTraceString(cause))
        if (!endsWith('\n')) append('\n')
    }

    private fun println(msg: String) {
        if (logdReady) {
            Log.i("HMA-OSS", msg)
        }
    }
}
