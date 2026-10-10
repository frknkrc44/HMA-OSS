package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;

import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;

import android.os.RemoteException;

import org.frknkrc44.hma_oss.zygote.callback.FindCallingAppsCallback;
import org.frknkrc44.hma_oss.zygote.service.ReturnValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import icu.nullptr.hidemyapplist.common.Constants;

public class PmsHookTargetBase extends ABaseFrameworkHook {

    PmsHookTargetBase(String tag) {
        super(tag);
    }

    private final AtomicReference<String> lastFilteredApp = new AtomicReference<>(null);

    protected void applyPackageHiding(
            String methodName,
            ReturnValue returnValue,
            Supplier<Integer> findCallingUid,
            Supplier<String> findTargetApp,
            FindCallingAppsCallback findCallingApps,
            Object valueForHiding
    ) throws RemoteException {
        if (returnValue.throwable != null) return;

        final var callingUid = findCallingUid.get();
        if (callingUid == null || callingUid == Constants.UID_SYSTEM) return;

        final var targetApp = findTargetApp.get();
        if (targetApp == null) return;

        logV(TAG, null, () -> String.format(Locale.US,
                "@%s incoming query: %d => %s",
                methodName, callingUid, targetApp
        ));

        if (service.dataHolder.shouldHideFromUid(callingUid, targetApp)) {
            returnValue.setResult(valueForHiding);
            service.increasePMFilterCount(callingUid);
            logD(TAG, null, () -> String.format(Locale.US,
                    "@%s caller cache: %d, target: %s",
                    methodName, callingUid, targetApp
            ));
            return;
        }

        final var callingUserId = getUserFromCallingUid(callingUid);
        final var callingApps = findCallingApps.accept(callingUid);
        if (callingApps == null) return;
        final var caller = Arrays.stream(callingApps)
                .filter(item -> service.shouldHide(item, targetApp, callingUserId))
                .findFirst()
                .orElse(null);
        if (caller == null) return;

        logD(TAG, null, () -> String.format(Locale.US,
                "@%s caller: %s, target: %s",
                methodName, caller, targetApp
        ));
        returnValue.setResult(valueForHiding);
        final var last = lastFilteredApp.getAndSet(caller);
        if (!caller.equals(last)) {
            logI(TAG, null, () -> String.format(Locale.US,
                    "@%s: query from caller: %s",
                    methodName, caller
            ));
        }
        service.dataHolder.putShouldHideUidCache(callingUid, caller, targetApp);
        service.increasePMFilterCount(caller);
    }
}
