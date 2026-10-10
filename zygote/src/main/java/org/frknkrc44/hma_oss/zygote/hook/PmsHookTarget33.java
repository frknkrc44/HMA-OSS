package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_IMPL_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstWithType;

import android.os.Binder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import icu.nullptr.hidemyapplist.common.OSUtils;

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class PmsHookTarget33 extends PmsHookTargetBase {

    public PmsHookTarget33() {
        super("PmsHookTarget33");
    }

    PmsHookTarget33(String tag) {
        super(tag);
    }

    @Override
    public void load() {
        super.load();

        if (!OSUtils.isSamsung()) {
            service.hooker.hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "addPackageHoldingPermissions",
                    (methodName, frame, returnValue) -> applyPackageHiding(
                            methodName,
                            returnValue,
                            Binder::getCallingUid,
                            () -> getPackageNameFromPackageSettings(getArgument(frame, 2)),
                            callingUid -> getCallingApps(service.pms, callingUid),
                            null
                    )
            );
        }

        service.hooker.hookBefore(
                APPS_FILTER_IMPL_CLASS,
                "shouldFilterApplication",
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        () -> (int) getArgument(frame, 2),
                        () -> getPackageNameFromPackageSettings(getArgument(frame, 4)),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        true
                )
        );

        service.hooker.hookAfter(
                COMPUTER_ENGINE_CLASS,
                "getPackageInfoInternal",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);

                    applyPackageHiding(
                            methodName,
                            returnValue,
                            () -> firstWithType(args, int.class),
                            () -> firstWithType(args, String.class),
                            callingUid -> getCallingApps(service.pms, callingUid),
                            null
                    );
                }
        );

        service.hooker.hookAfter(
                COMPUTER_ENGINE_CLASS,
                "getApplicationInfoInternal",
                (methodName, frame, returnValue) -> {
                    final var args = dumpArgs(frame, true);

                    applyPackageHiding(
                            methodName,
                            returnValue,
                            () -> firstWithType(args, int.class),
                            () -> firstWithType(args, String.class),
                            callingUid -> getCallingApps(service.pms, callingUid),
                            null
                    );
                }
        );
    }
}
