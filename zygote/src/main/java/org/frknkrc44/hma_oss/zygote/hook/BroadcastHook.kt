package org.frknkrc44.hma_oss.zygote.hook

import android.content.Intent
import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstWithType
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_SERVICE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_CONTROLLER_CLASS

class BroadcastHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "BroadcastHook"

    companion object {
        private val fakeReturnCode by lazy {
            getStaticIntField(
                "android.app.ActivityManager",
                "BROADCAST_SUCCESS",
            )
        }
    }

    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    BROADCAST_CONTROLLER_CLASS
                } else {
                    ACTIVITY_MANAGER_SERVICE_CLASS
                },
                "broadcastIntentLocked",
            ) { param ->
                val caller = param.args.firstWithType<String?>() ?: return@hookBefore
                val intent = param.args.firstWithType<Intent?>() ?: return@hookBefore
                val targetApp = intent.component?.packageName

                if (service.shouldHideActivityLaunch(caller, targetApp)) {
                    logD(TAG) { "@broadcastIntent: insecure query from $caller, target: ${intent.component}" }
                    param.result = fakeReturnCode
                    service.increaseALFilterCount(caller)
                }
            }
        }
    }
}