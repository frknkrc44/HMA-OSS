package icu.nullptr.hidemyapplist.common

/** Shared by separately loaded backend class loaders in the same system_server process. */
object BackendRegistry {
    const val EXTRA_BACKEND = "hma.backend"
    const val EXTRA_API_VERSION = "hma.backend.api_version"
    private const val OWNER_PROPERTY = "org.frknkrc44.hma_oss.backend.owner"

    @JvmStatic
    fun claim(backend: String): Boolean {
        // A Kotlin singleton alone would be different in each module class loader.
        val properties = System.getProperties()
        return synchronized(properties) {
            if (properties.containsKey(OWNER_PROPERTY)) {
                false
            } else {
                properties.setProperty(OWNER_PROPERTY, backend)
                true
            }
        }
    }

    @JvmStatic
    fun owner(): String? = System.getProperty(OWNER_PROPERTY)
}
