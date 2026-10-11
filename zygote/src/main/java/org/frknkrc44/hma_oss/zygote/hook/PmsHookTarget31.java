package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PMS_COMPUTER_TRACKER_CLASS;

import android.os.Binder;

public class PmsHookTarget31 extends PmsHookTargetBase {

    public PmsHookTarget31() {
        super("PmsHookTarget31");
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                PACKAGE_MANAGER_SERVICE_CLASS,
                "getPackageSetting",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        Binder::getCallingUid,
                        () -> (String) getArgument(frame, 1),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        true
                )
        );

        service.hooker.hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageSettingInternal",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 2),
                        () -> (String) getArgument(frame, 1),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        true
                )
        );

        service.hooker.hookBefore(
                APPS_FILTER_CLASS,
                "shouldFilterApplication",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 1),
                        () -> getPackageNameFromPackageSettings(getArgument(frame, 3)),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        true
                )
        );

        service.hooker.hookAfter(
                PMS_COMPUTER_TRACKER_CLASS,
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

        service.hooker.hookAfter(
                PMS_COMPUTER_TRACKER_CLASS,
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
