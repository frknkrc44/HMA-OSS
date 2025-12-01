package icu.nullptr.hidemyapplist.ui.util

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableSharedFlow
import org.frknkrc44.hma_oss.R

fun Boolean.enabledString(resources: Resources, lower: Boolean = false): String {
    val returnedStr = if (this) resources.getString(R.string.enabled)
    else resources.getString(R.string.disabled)

    return if (lower) returnedStr.lowercase() else returnedStr
}

fun ActivityInfo.asComponentName() = ComponentName(packageName, name)

fun <T> MutableSharedFlow<T>.get() = replayCache.first()
