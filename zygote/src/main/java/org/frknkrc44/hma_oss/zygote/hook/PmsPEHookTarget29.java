package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS;

import android.os.Bundle;

public class PmsPEHookTarget29 extends ABaseFrameworkHook {

    public PmsPEHookTarget29() {
        super("PmsPEHookTarget29");
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "sendPackageBroadcast",
                (methodName, frame, returnValue) -> service.handlePackageEvent(
                        (String) getArgument(frame, 1),
                        (String) getArgument(frame, 2),
                        (Bundle) getArgument(frame, 3)
                )
        );
    }
}
