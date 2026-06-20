package org.frknkrc44.hma_oss.zygote.hook

import android.content.Intent
import android.os.Build
import android.os.Bundle
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArg
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

class PmsPackageEventsHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "PmsPackageEventsHook"

    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hookBefore(
                    "com.android.server.pm.BroadcastHelper",
                    "sendPackageBroadcastAndNotify",
                ) { _, _, frame, _ ->
                    service.handlePackageEvent(
                        frame.getArg(1) as String?,
                        frame.getArg(2) as String?,
                        frame.getArg(3) as Bundle?,
                    )
                }

                hookBefore(
                    "com.android.internal.content.PackageMonitor",
                    "onReceive",
                ) { _, _, frame, _ ->
                    val intent = frame.getArg(2) as? Intent? ?: return@hookBefore

                    service.handlePackageEvent(
                        intent.action,
                        intent.data?.encodedSchemeSpecificPart,
                        intent.extras,
                    )
                }
            } else {
                hookBefore(
                    PACKAGE_MANAGER_SERVICE_CLASS,
                    "sendPackageBroadcast",
                ) { _, _, frame, _ ->
                    service.handlePackageEvent(
                        frame.getArg(1) as String?,
                        frame.getArg(2) as String?,
                        frame.getArg(3) as Bundle?,
                    )
                }
            }
        }
    }
}
