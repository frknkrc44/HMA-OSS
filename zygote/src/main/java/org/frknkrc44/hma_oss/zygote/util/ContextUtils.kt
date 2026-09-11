package org.frknkrc44.hma_oss.zygote.util

import android.app.ActivityThread

object ContextUtils {
    val application get() = ActivityThread.currentActivityThread().application!!

    val packageManager get() = application.packageManager!!

    val contentResolver get() = application.contentResolver!!
}
