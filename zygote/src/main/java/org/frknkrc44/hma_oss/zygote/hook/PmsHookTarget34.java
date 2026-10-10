package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS;

import android.os.Binder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class PmsHookTarget34 extends PmsHookTarget33 {

    public PmsHookTarget34() {
        super("PmsHookTarget34");
    }

    @Override
    public void load() {
        super.load();

        final var getAPMethod = (Method) service.hooker.findAltMethod(
                List.of(PACKAGE_MANAGER_SERVICE_CLASS),
                List.of("getArchivedPackageInternal", "getArchivedPackage")
        );
        service.hooker.hookAfter(
                getAPMethod,
                (methodName, frame, returnValue) -> applyPackageHiding(
                        methodName,
                        returnValue,
                        Binder::getCallingUid,
                        () -> (String) getArgument(frame, 1),
                        callingUid -> getCallingApps(service.pms, callingUid),
                        null
                )
        );
    }
}
