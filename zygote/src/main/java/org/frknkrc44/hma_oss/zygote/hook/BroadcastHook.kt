package org.frknkrc44.hma_oss.zygote.hook

import android.content.Intent
import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import org.frknkrc44.hma_oss.zygote.service.UserService.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTION_USB_STATE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_SERVICE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_CONTROLLER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.USB_FUNCTION_ADB

class BroadcastHook : IFrameworkHook {
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

        service!!.hookerInstance.apply {
            hookBefore(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    BROADCAST_CONTROLLER_CLASS
                } else {
                    ACTIVITY_MANAGER_SERVICE_CLASS
                },
                "broadcastIntentLocked",
            ) { _, frame, returnValue ->
                val userId = frame.args.lastOrNullWithType<Int>() ?: return@hookBefore
                val caller = frame.args.firstOrNullWithType<String>() ?: return@hookBefore
                val intent = frame.args.firstOrNullWithType<Intent>() ?: return@hookBefore
                val targetApp = intent.component?.packageName

                if (service?.shouldHideActivityLaunch(caller, targetApp, userId) ?: false) {
                    logD(TAG) { "@broadcastIntent: insecure query from $caller, target: ${intent.component}" }
                    returnValue.result = fakeReturnCode
                    service?.increaseALFilterCount(caller)
                }

                changeUsbStateBroadcast(intent)
            }
        }
    }

    private fun changeUsbStateBroadcast(intent: Intent) {
        if (service?.config?.disableActivityLaunchProtection ?: false) return

        if (intent.action == ACTION_USB_STATE) {
            intent.removeExtra(USB_FUNCTION_ADB)
        }
    }
}
