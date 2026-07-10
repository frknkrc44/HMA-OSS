package org.frknkrc44.hma_oss.zygote.hook

interface IFrameworkHook {
    @Suppress("PropertyName")
    val TAG: String

    fun load()
}
