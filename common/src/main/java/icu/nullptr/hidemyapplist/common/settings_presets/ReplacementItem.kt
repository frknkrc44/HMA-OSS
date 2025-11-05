package icu.nullptr.hidemyapplist.common.settings_presets

import kotlinx.serialization.Serializable

@Serializable
data class ReplacementItem(
    val name: String,
    val value: String?,
    val database: String = BasePreset.SETTINGS_GLOBAL,
) {
    override fun toString() = "ReplacementItem { " +
            "'name': '$name', " +
            "'value': '$value', " +
            // "'database': '$database'" +
            " }"
}