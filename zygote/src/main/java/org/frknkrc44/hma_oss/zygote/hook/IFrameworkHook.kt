package org.frknkrc44.hma_oss.zygote.hook

import org.frknkrc44.hma_oss.zygote.service.UserService

interface IFrameworkHook {
    @Suppress("PropertyName")
    val TAG: String

    val service get() = UserService.service!!
    val hookerInstance get() = service.hookerInstance
    val pms get() = service.pms
    val config get() = service.config
    val systemApps get() = service.systemApps

    fun load()
}
