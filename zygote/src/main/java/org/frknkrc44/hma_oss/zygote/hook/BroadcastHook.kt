package org.frknkrc44.hma_oss.zygote.hook

import android.content.ComponentName
import android.content.IIntentReceiver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getBooleanField
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
        logI(TAG) { "Load hook" }

        hooker.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                hookBefore(
                    BROADCAST_PROCESS_QUEUE_CLASS,
                    "enqueueOutgoingBroadcast",
                    hook = ::enqueueBroadcastLocked,
                )

                hookBefore(
                    BROADCAST_PROCESS_QUEUE_CLASS,
                    "enqueueOrReplaceBroadcast",
                    hook = ::enqueueBroadcastLocked,
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
                    hook = ::enqueueBroadcastLocked,
                )

                hookBefore(
                    targetClass,
                    "enqueueOrderedBroadcastLocked",
                    hook = ::enqueueBroadcastLocked,
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
                val intent = frame.args.firstOrNullWithType<Intent>() ?: return@hookBefore
                changeUsbStateBroadcast(intent)
            }
        }
    }

    private fun enqueueBroadcastLocked(
        methodName: String,
        frame: EmulatedStackFrame,
        returnValue: ReturnValue,
    ) {
        val record = frame.getArgument(1)
        val caller = getObjectField(record, "callerPackage") as? String ?: return
        val component = getObjectField(record, "targetComp") as? ComponentName ?: return
        val targetApp = component.packageName
        val userId = getIntField(record, "userId")

        if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
            logD(TAG) { "@$methodName: insecure query from $caller, target: $component" }
            returnValue.result = null

            val resultTo = getObjectField(record, "resultTo") as? IIntentReceiver
            val haveATarget = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getObjectField(record, "callerApp") != null
            } else {
                getObjectField(record, "resultToApp") != null
            }

            if (resultTo != null && haveATarget) {
                resultTo.performReceive(
                    getObjectField(record, "intent") as Intent,
                    getIntField(record, "resultCode"),
                    getObjectField(record, "resultData") as? String,
                    getObjectField(record, "resultExtras") as? Bundle,
                    getBooleanField(record, "ordered"),
                    getBooleanField(record, "sticky"),
                    userId,
                )
            }

            service.increaseALFilterCount(caller)
        }
    }

    private fun changeUsbStateBroadcast(intent: Intent) {
        if (config.disableActivityLaunchProtection) return

        if (intent.action == ACTION_USB_STATE) {
            intent.removeExtra(USB_FUNCTION_ADB)
        }
    }
}
