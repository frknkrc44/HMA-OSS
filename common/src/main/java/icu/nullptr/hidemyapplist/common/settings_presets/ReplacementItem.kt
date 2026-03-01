package top.secret.hma.v1.common.settings_presets

import top.secret.hma.v1.common.Constants
import kotlinx.serialization.Serializable

@Serializable
data class ReplacementItem(
    val name: String,
    val value: String?,
    val database: String = Constants.SETTINGS_GLOBAL,
) {
    override fun toString() = "ReplacementItem { " +
            "'name': '$name', " +
            "'value': '$value', " +
            // "'database': '$database'" +
            " }"
}
