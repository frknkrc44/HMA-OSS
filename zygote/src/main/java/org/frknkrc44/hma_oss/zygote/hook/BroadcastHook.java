package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTION_USB_STATE;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_SERVICE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_CONTROLLER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_PROCESS_QUEUE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_QUEUE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_QUEUE_IMPL_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.USB_FUNCTION_ADB;

import static icu.nullptr.hidemyapplist.common.Utils.generateRandomHex;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstOrNullWithType;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import com.v7878.unsafe.invoke.EmulatedStackFrame;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.callback.HookCallback;
import org.frknkrc44.hma_oss.zygote.service.ReturnValue;

public class BroadcastHook extends ABaseFrameworkHook {

    public BroadcastHook() {
        super("BroadcastHook");
    }

    @Override
    public void load() {
        super.load();

        final var callback = new Callback();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            service.hooker.hookBefore(BROADCAST_PROCESS_QUEUE_CLASS, "enqueueOutgoingBroadcast", callback);
            service.hooker.hookBefore(BROADCAST_PROCESS_QUEUE_CLASS, "enqueueOrReplaceBroadcast", callback);
        } else {
            final var targetClass = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ? BROADCAST_QUEUE_CLASS
                    : BROADCAST_QUEUE_IMPL_CLASS;

            service.hooker.hookBefore(targetClass, "enqueueParallelBroadcastLocked", callback);
            service.hooker.hookBefore(targetClass, "enqueueOrderedBroadcastLocked", callback);
        }

        // replace USB state receiver
        service.hooker.hookBefore(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                        ? BROADCAST_CONTROLLER_CLASS
                        : ACTIVITY_MANAGER_SERVICE_CLASS,
                "broadcastIntentLocked",
                (methodName, frame, returnValue) -> {
                    if (service.config.getDisableActivityLaunchProtection()) return;

                    final var args = dumpArgs(frame, true);
                    final var intent = firstOrNullWithType(args, Intent.class);
                    if (intent != null && ACTION_USB_STATE.equals(intent.getAction())) {
                        intent.removeExtra(USB_FUNCTION_ADB);
                    }
                }
        );
    }

    private class Callback implements HookCallback {

        @Override
        public void accept(String methodName, EmulatedStackFrame frame, ReturnValue returnValue) throws Throwable {
            final var record = getArgument(frame, 1);
            final var caller = (String) getObjectField(record, "callerPackage");
            if (caller == null) return;
            final var component = (ComponentName) getObjectField(record, "targetComp");
            if (component == null) return;
            final var targetApp = component.getPackageName();
            final var userId = getIntField(record, "userId");

            if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
                logD(TAG, null, () -> "@" + methodName + ": insecure query from " + caller + ", target: " + component);

                Intent intent = (Intent) getObjectField(record, "intent");
                if (intent != null) {
                    intent.setComponent(null);
                    intent.setPackage(BuildConfig.APP_PACKAGE_NAME + "." + generateRandomHex(4));
                }

                service.increaseALFilterCount(caller);
            }
        }
    }
}
