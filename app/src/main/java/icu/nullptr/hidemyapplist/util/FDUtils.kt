package icu.nullptr.hidemyapplist.util

import android.os.ParcelFileDescriptor
import android.util.Log
import icu.nullptr.hidemyapplist.service.ServiceClient.log
import kotlin.concurrent.thread

object FDUtils {
    private const val TAG = "FDUtils"

    fun writeIntoPipe(text: String): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val reader = pipe[0]
        val writer = pipe[1]

        thread {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writer)
                    .bufferedWriter(Charsets.UTF_8).use { output ->
                    output.write(text)
                    output.flush()
                }
            } catch (cause: Throwable) {
                log(Log.ERROR, TAG, cause.stackTraceToString())
            }
        }

        return reader
    }
}
