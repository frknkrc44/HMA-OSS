package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FilterHolder(
    val filterCounts: MutableMap<String, FilterCount> = mutableMapOf(),
) {
    @Serializable
    data class FilterCount(
        var packageManagerCount: Int = 0,
        var activityLaunchCount: Int = 0,
        var installerCount: Int = 0,
        var settingsCount: Int = 0,
        var othersCount: Int = 0,
    ) {
        val totalCount: Int get() = packageManagerCount +
                activityLaunchCount +
                installerCount +
                settingsCount +
                othersCount
    }

    val totalCount: Int get() = filterCounts.values.sumOf { it.totalCount }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<FilterHolder>(json)

        private val encoder = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    enum class FilterType {
        PACKAGE_MANAGER,
        ACTIVITY_LAUNCH,
        INSTALLER,
        SETTINGS,
        OTHERS,
    }

    override fun toString() = encoder.encodeToString(this)
}
