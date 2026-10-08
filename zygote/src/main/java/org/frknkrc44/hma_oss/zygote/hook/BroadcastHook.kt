package org.frknkrc44.hma_oss.zygote.hook

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.Utils.generateRandomHex
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.zygote.service.HookCallback
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTION_USB_STATE
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_SERVICE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_CONTROLLER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_PROCESS_QUEUE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_QUEUE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_QUEUE_IMPL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.USB_FUNCTION_ADB

class BroadcastHook : IFrameworkHook {
    override val TAG = "BroadcastHook"

    override fun load() {
        logI(TAG, null) { "Load hook" }

        hooker.apply {
            val callback = Callback()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                hookBefore(
                    BROADCAST_PROCESS_QUEUE_CLASS,
                    "enqueueOutgoingBroadcast",
                    callback,
                )

                hookBefore(
                    BROADCAST_PROCESS_QUEUE_CLASS,
                    "enqueueOrReplaceBroadcast",
                    callback,
                )
            } else {
                val targetClass = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    BROADCAST_QUEUE_CLASS
                } else {
                    BROADCAST_QUEUE_IMPL_CLASS
                }

                hookBefore(
                    targetClass,
                    "enqueueParallelBroadcastLocked",
                    callback,
                )

                hookBefore(
                    targetClass,
                    "enqueueOrderedBroadcastLocked",
                    callback,
                )
            }

            // replace USB state receiver
            hookBefore(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    BROADCAST_CONTROLLER_CLASS
                } else {
                    ACTIVITY_MANAGER_SERVICE_CLASS
                },
                "broadcastIntentLocked",
            ) { _, frame, _ ->
                val intent = dumpArgs(frame, true).firstOrNullWithType<Intent>() ?: return@hookBefore
                changeUsbStateBroadcast(intent)
            }
        }
    }

    private inner class Callback : HookCallback {
        override fun accept(methodName: String?, frame: EmulatedStackFrame?, returnValue: ReturnValue?) {
            val record = getArgument(frame, 1)
            val caller = getObjectField(record, "callerPackage") as? String ?: return
            val component = getObjectField(record, "targetComp") as? ComponentName ?: return
            val targetApp = component.packageName
            val userId = getIntField(record, "userId")

            if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
                logD(TAG, null) { "@$methodName: insecure query from $caller, target: $component" }

                (getObjectField(record, "intent") as Intent).apply {
                    this.component = null
                    this.`package` = "${BuildConfig.APP_PACKAGE_NAME}.${generateRandomHex(4)}"
                }

                service.increaseALFilterCount(caller)
            }
        }
    }

    private fun changeUsbStateBroadcast(intent: Intent) {
        if (config.disableActivityLaunchProtection) return

        if (intent.action == ACTION_USB_STATE) {
            intent.removeExtra(USB_FUNCTION_ADB)
        }
    }
}
