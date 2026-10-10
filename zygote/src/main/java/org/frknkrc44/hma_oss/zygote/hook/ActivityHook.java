package org.frknkrc44.hma_oss.zygote.hook;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;
import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getScopedCaller;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getThisObject;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_STARTER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PMS_COMPUTER_ENGINE_CLASS;
import static icu.nullptr.hidemyapplist.common.Utils.getPackageName;
import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstOrNullWithType;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstWithType;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import icu.nullptr.hidemyapplist.common.Constants;

public class ActivityHook extends ABaseFrameworkHook {

    public ActivityHook() throws ClassNotFoundException, IllegalAccessException {
        super("ActivityHook");

        fakeRC = getStaticIntField(ACTIVITY_MANAGER_CLASS, "START_CLASS_NOT_FOUND");
    }

    private final int fakeRC;

    private static final String APRF_METHOD = "applyPostResolutionFilter";

    @Override
    public void load() {
        super.load();

        final var aPRFClazz = switch (VERSION.SDK_INT) {
            case VERSION_CODES.Q, VERSION_CODES.R -> PACKAGE_MANAGER_SERVICE_CLASS;
            case VERSION_CODES.S, VERSION_CODES.S_V2 -> PMS_COMPUTER_ENGINE_CLASS;
            default -> COMPUTER_ENGINE_CLASS;
        };

        service.hooker.hookAfter(
                aPRFClazz,
                APRF_METHOD,
                (methodName, frame, returnValue) -> {
                    final var result = returnValue.getResult();
                    if (!(result instanceof List) || ((List<?>) result).isEmpty()) return;

                    @SuppressWarnings("unchecked")
                    final var list = (List<ResolveInfo>) result;

                    final var args = dumpArgs(frame, true);
                    final var callingUid = firstWithType(args, int.class);
                    if (callingUid == Constants.UID_SYSTEM) return;

                    final var callingUserId = getUserFromCallingUid(callingUid);
                    final var callingApps = getCallingApps(service.pms, callingUid);
                    final var caller = getScopedCaller(callingApps, service::isHookEnabled);
                    if (caller == null) return;

                    logV(TAG, null, () -> String.format(
                            "@%s: %s requested a resolve info",
                            methodName, caller
                    ));

                    final var removedList = new ArrayList<ResolveInfo>();
                    for (ResolveInfo info : list) {
                        final var targetApp = getPackageName(info);

                        logV(TAG, null, () -> String.format(
                                "@%s: Checking %s for %s",
                                methodName, targetApp, caller
                        ));

                        final var hide = service
                                .shouldHideActivityLaunch(caller, targetApp, callingUserId);

                        if (hide) {
                            logD(TAG, null, () -> String.format(
                                    "@%s: insecure query from %s, target %s",
                                    methodName, caller, targetApp
                            ));

                            removedList.add(info);
                        }
                    }

                    if (!removedList.isEmpty()) {
                        // OneUI uses the list it passed in instead of the returned one,
                        // so drop the entries from that object instead
                        list.removeAll(removedList);

                        service.increasePMFilterCount(caller, removedList.size());

                        returnValue.setResult(list);
                    }
                }
        );

        // we don't need the execute/executeRequest/startActivity hooks in that case
        if (service.hooker.isHookAvailable(aPRFClazz, APRF_METHOD)) return;

        boolean isInxLockerAvailable = false;
        try {
            isInxLockerAvailable = service.pms.isPackageAvailable(
                    "io.github.chimio.inxlocker", 0);
        } catch (RemoteException ignore) {}

        if (isInxLockerAvailable) {
            if (VERSION.SDK_INT >= VERSION_CODES.R) {
                service.hooker.hookBefore(
                        ACTIVITY_STARTER_CLASS,
                        "executeRequest",
                        (methodName, frame, returnValue) -> {
                            final var request = getArgument(frame, 1);
                            final var userId = getUserFromCallingUid(getIntField(request, "callingUid"));
                            final var caller = (String) getObjectField(request, "callingPackage");
                            final var intent = (Intent) getObjectField(request, "intent");
                            if (intent == null || intent.getComponent() == null) return;
                            final var targetApp = intent.getComponent().getPackageName();

                            if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
                                logD(TAG, null, () -> String.format(
                                        "@%s: insecure query from %s, target: %s",
                                        methodName, caller, targetApp
                                ));
                                returnValue.setResult(fakeRC);
                                service.increaseALFilterCount(caller);
                            }
                        }
                );
            } else {
                service.hooker.hookBefore(
                        ACTIVITY_STARTER_CLASS,
                        "startActivity",
                        (methodName, frame, returnValue) -> {
                            final var args = dumpArgs(frame, true);
                            final var userId = getUserFromCallingUid((int) args[12]);
                            final var caller = firstOrNullWithType(args, String.class);
                            final var intent = firstOrNullWithType(args, Intent.class);
                            if (intent == null || intent.getComponent() == null) return;
                            final var targetApp = intent.getComponent().getPackageName();

                            if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
                                logD(TAG, null, () -> String.format(
                                        "@%s: insecure query from %s, target: %s",
                                        methodName, caller, targetApp
                                ));
                                returnValue.setResult(fakeRC);
                                service.increaseALFilterCount(caller);
                            }
                        }
                );
            }
        } else {
            service.hooker.hookBefore(
                    ACTIVITY_STARTER_CLASS,
                    "execute",
                    (methodName, frame, returnValue) -> {
                        final var request = getObjectField(getThisObject(frame), "mRequest");
                        if (request == null) return;

                        final var userId = getUserFromCallingUid(getIntField(request, "callingUid"));
                        final var caller = (String) getObjectField(request, "callingPackage");
                        final var intent = (Intent) getObjectField(request, "intent");
                        if (intent == null || intent.getComponent() == null) return;
                        final var targetApp = intent.getComponent().getPackageName();

                        if (service.shouldHideActivityLaunch(caller, targetApp, userId)) {
                            logD(TAG, null, () -> String.format(
                                    "@%s: insecure query from %s, target: %s",
                                    methodName, caller, targetApp
                            ));
                            returnValue.setResult(fakeRC);
                            service.increaseALFilterCount(caller);
                        }
                    }
            );
        }
    }
}
