package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.BROADCAST_HELPER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MONITOR_CLASS;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class PmsPEHookTarget33 extends ABaseFrameworkHook {

    public PmsPEHookTarget33() {
        super("PmsPEHookTarget33");
    }

    @Override
    public void load() {
        super.load();

        final var hookedMethodName = "sendPackageBroadcastAndNotify";

        service.hooker.hookBefore(
                BROADCAST_HELPER_CLASS,
                hookedMethodName,
                (methodName, frame, returnValue) -> service.handlePackageEvent(
                        (String) getArgument(frame, 1),
                        (String) getArgument(frame, 2),
                        (Bundle) getArgument(frame, 3)
                )
        );

        if (!service.hooker.isHookAvailable(BROADCAST_HELPER_CLASS, hookedMethodName)) {
            service.hooker.hookBefore(
                    PACKAGE_MONITOR_CLASS,
                    "onReceive",
                    (methodName, frame, returnValue) -> {
                        final var intent = (Intent) getArgument(frame, 2);
                        if (intent == null) return; // make Android Studio happy

                        final var data = intent.getData();
                        final var part = data != null
                                ? data.getEncodedSchemeSpecificPart()
                                : null;

                        service.handlePackageEvent(
                                intent.getAction(),
                                part,
                                intent.getExtras()
                        );
                    }
            );
        }
    }
}
