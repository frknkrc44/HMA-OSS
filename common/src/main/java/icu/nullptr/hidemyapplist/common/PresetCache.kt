package icu.nullptr.hidemyapplist.common

import icu.nullptr.hidemyapplist.common.Utils.encoder
import kotlinx.serialization.Serializable

@Serializable
data class PresetCache(
    val cache: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val riskyPackageCache: MutableSet<String> = mutableSetOf(),
) {
    companion object {
        fun parse(json: String) = encoder.decodeFromString<PresetCache>(json)

    }

    override fun toString() = encoder.encodeToString(this)
}
