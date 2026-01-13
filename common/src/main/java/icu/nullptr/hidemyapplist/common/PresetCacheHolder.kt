package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PresetCacheHolder(
    val cacheVersion: Int = -1,
    val presetPackageNames: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val gmsDependentApps: MutableSet<String> = mutableSetOf(),
) {
    companion object {
        fun parse(json: String) = encoder.decodeFromString<PresetCacheHolder>(json)

        private val encoder = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    override fun toString() = encoder.encodeToString(this)
}