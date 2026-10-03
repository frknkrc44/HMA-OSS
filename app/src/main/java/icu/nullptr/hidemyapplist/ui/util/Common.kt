package icu.nullptr.hidemyapplist.ui.util

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.util.TypedValue
import kotlinx.coroutines.flow.MutableSharedFlow
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R

fun Boolean.enabledString(resources: Resources, lower: Boolean = false): String {
    val returnedStr = if (this) resources.getString(R.string.enabled)
    else resources.getString(R.string.disabled)

    return if (lower) returnedStr.lowercase() else returnedStr
}

fun ActivityInfo.asComponentName() = ComponentName(packageName, name)

fun <T> MutableSharedFlow<T>.get() = replayCache.first()

inline val Int.dpToPx: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val isTestBuild get() = BuildConfig.VERSION_NAME.let { name ->
    name.count { it == '-' } != 1 ||
    name.count { it == '+' } > 0 ||
    name.split('-').last().length == 8
}
