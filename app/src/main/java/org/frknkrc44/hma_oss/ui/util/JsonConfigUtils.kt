package org.frknkrc44.hma_oss.ui.util

import android.os.Bundle
import icu.nullptr.hidemyapplist.common.settings_presets.ReplacementItem

object JsonConfigUtils {
    fun Collection<ReplacementItem>.targetSettingListToBundle() = Bundle().apply {
        forEach { item ->
            putStringArray(item.name, arrayOf(item.value, item.database))
        }
    }

    fun Bundle.toTargetSettingList() = keySet().mapNotNull {
        val item = getStringArray(it) ?: return@mapNotNull null
        ReplacementItem(it, item[0], item[1])
    }
}
