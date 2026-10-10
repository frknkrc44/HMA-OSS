package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS;

public class PmsHookTarget29 extends PmsHookTargetBase {

    public PmsHookTarget29() {
        super("PmsHookTarget29");
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                service.pms.getClass().getName(),
                "filterAppAccessLPr",
                5,
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 2),
                        () -> getPackageNameFromPackageSettings(getArgument(frame, 1)),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        true
                )
        );

        service.hooker.hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getPackageInfoInternal",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 4),
                        () -> (String) getArgument(frame, 1),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        null
                )
        );

        service.hooker.hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getApplicationInfoInternal",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 3),
                        () -> (String) getArgument(frame, 1),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        null
                )
        );
    }
}
