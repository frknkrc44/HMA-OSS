package top.secret.hma.v1.ui.util

import android.os.Bundle
import top.secret.hma.v1.common.settings_presets.ReplacementItem
import top.secret.hma.v1.ui.fragment.EditSettingFragmentArgs

fun ReplacementItem.toEditSettingFragmentArgs() = EditSettingFragmentArgs(
    database = database,
    name = name,
    value = value,
)

fun List<ReplacementItem>.targetSettingListToBundle() = Bundle().apply {
    forEach { item ->
        putStringArray(item.name, arrayOf(item.value, item.database))
    }
}

fun Bundle.toTargetSettingList() = keySet().mapNotNull {
    val item = getStringArray(it) ?: return@mapNotNull null
    ReplacementItem(it, item[0], item[1])
}
