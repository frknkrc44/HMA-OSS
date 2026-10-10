package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.isAppDataIsolationEnabled;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgTypes;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.shortyEquals;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONSTRUCTOR_METHOD_NAME;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.NATIVE_ZYGOTE_PROCESS_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.SERVICE_RECORD_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS;

import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.firstOrNullWithType;
import static icu.nullptr.hidemyapplist.common.util.CollectionUtils.lastOrNullWithType;

import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.ProcessParams;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.v7878.unsafe.ArtMethodUtils;
import com.v7878.unsafe.invoke.EmulatedStackFrame;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import icu.nullptr.hidemyapplist.common.Constants;

public class ZygoteHook extends ABaseFrameworkHook {

    public ZygoteHook() {
        super("ZygoteHook");
    }

    private final AtomicReference<String> lastForceMountedApp = new AtomicReference<>(null);

    private boolean isForceMountData() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                service.config.getForceMountData() &&
                isAppDataIsolationEnabled(service.config);
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                ZYGOTE_PROCESS_CLASS,
                "start",
                (methodName, frame, returnValue) -> hookIntoZygoteProcess(frame)
        );

        // TODO: Try to find a way for Android 12- compatibility without harming TANGO support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Try to fix PrivIsolated
            service.hooker.hookBefore(
                    SERVICE_RECORD_CLASS,
                    CONSTRUCTOR_METHOD_NAME,
                    (methodName, frame, returnValue) -> {
                        final var args = dumpArgs(frame, true);
                        final var caller = firstOrNullWithType(args, String.class);
                        if (caller == null) return;

                        final var perms = service.getRestrictedZygotePermissions(caller);
                        if (perms == null || !perms.contains(Constants.APP_ZYGOTE_GID)) return;

                        final var serviceInfo = firstOrNullWithType(args, ServiceInfo.class);
                        if (serviceInfo == null) return;

                        final var flags = serviceInfo.flags;
                        if ((flags & ServiceInfo.FLAG_ISOLATED_PROCESS) == 0) return;
                        if ((flags & ServiceInfo.FLAG_NATIVE_SERVICE) != 0) return;

                        logD(TAG, null, () -> "@serviceRecord: Isolated process becomes app zygote process for " + caller + " service");
                        serviceInfo.flags |= ServiceInfo.FLAG_USE_APP_ZYGOTE;
                    }
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            service.hooker.hookBefore(
                    NATIVE_ZYGOTE_PROCESS_CLASS,
                    "start",
                    (methodName, frame, returnValue) -> hookIntoZygoteProcess(frame)
            );
        }
    }

    private void hookIntoZygoteProcess(EmulatedStackFrame frame) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        final var args = dumpArgs(frame, true);
        final var isModern = args.length < 3;
        logD(TAG, null, () -> "@startZygoteProcess: Starting " + Arrays.toString(args) + ", modern: " + isModern);

        if (isModern) {
            hookIntoZygoteProcessModern(frame, args);
        } else {
            hookIntoZygoteProcessLegacy(frame, args);
        }
    }

    private void hookIntoZygoteProcessLegacy(EmulatedStackFrame frame, Object[] args) {
        final var caller = lastOrNullWithType(args, String.class);
        if (caller == null) return;

        final var isHookEnabled = service.isHookEnabled(caller);
        if (!isHookEnabled) return;

        // another plan for PlatformCompatHook
        final var argTypes = dumpArgTypes(frame, true);
        final var pair = getForceMountArgs(caller, args, argTypes);
        if (pair.first) {
            for (int i = 0; i < argTypes.length; i++) {
                final var clazz = argTypes[i];
                if (clazz == Map.class) {
                    final var bindMountAppsDataIndex = i + 1;
                    if (shortyEquals(frame, bindMountAppsDataIndex, 'Z')) {
                        final var last = lastForceMountedApp.getAndSet(caller);
                        if (!caller.equals(last)) {
                            logI(TAG, null, () -> "@startZygoteProcessLegacy: force mountAppsData for " + caller);
                        }
                        setArgument(frame, bindMountAppsDataIndex, true);
                        logD(TAG, null, () -> "@startZygoteProcessLegacy: mountAppsData argument overridden for " + caller);
                    }
                }
            }
        }

        if (pair.second < 0) return;

        final var perms = getRestrictedZygotePermissions(caller);
        if (perms != null && !perms.isEmpty()) {
            final var gIDs = (int[]) args[pair.second];
            if (gIDs == null) return;

            logD(TAG, null, () -> "@startZygoteProcessLegacy: GIDs are " + Arrays.toString(gIDs) + ", removing " + perms + " now");
            setArgument(frame, pair.second, Arrays.stream(gIDs).filter(e -> !perms.contains(e)).toArray());
            service.increaseOthersFilterCount(caller);
        }
    }

    /**
     * This method is added on Android 17 QPR3 Beta 1
     */
    @SuppressWarnings("ConstantValue")
    private void hookIntoZygoteProcessModern(EmulatedStackFrame frame, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        final var processParams = (ProcessParams) args[0];

        final var caller = processParams.packageName;
        final var isHookEnabled = service.isHookEnabled(caller);
        if (!isHookEnabled) return;

        ProcessParams.Builder builder = null;
        if (processParams.targetSdkVersion < Build.VERSION_CODES.R && processParams.isTopApp) {
            builder = makeProcessParamsBuilder(processParams);
            builder.setBindMountAppsData(true);
        }

        if (processParams.gids == null) {
            if (builder != null) {
                setArgument(frame, 1, builder.build());
            }

            return;
        }

        final var perms = getRestrictedZygotePermissions(caller);
        if (perms != null && !perms.isEmpty()) {
            final var gIDs = processParams.gids;
            logD(TAG, null, () -> "@startZygoteProcessModern: GIDs are " + Arrays.toString(gIDs) + ", removing " + perms + " now");

            if (builder == null) builder = makeProcessParamsBuilder(processParams);
            builder.setGids(Arrays.stream(gIDs).filter(e -> !perms.contains(e)).toArray());
            service.increaseOthersFilterCount(caller);
        }

        if (builder != null) {
            setArgument(frame, 1, builder.build());
        }
    }

    private Pair<Boolean, Integer> getForceMountArgs(String caller, Object[] args, Class<?>[] argTypes) {
        var gIDsVarIndex = -1;
        for (int i = 0; i < argTypes.length; i++) {
            final var clazz = argTypes[i];
            if (clazz == int[].class) {
                gIDsVarIndex = i;
                continue;
            }

            if (gIDsVarIndex < 0) continue;

            if (!isForceMountData() || service.systemApps.contains(caller)) {
                return new Pair<>(false, gIDsVarIndex);
            }

            if (clazz == String.class) {
                final var targetSDKVar = args[i - 1];
                if (targetSDKVar instanceof Integer var) {
                    if (var >= 30) return new Pair<>(false, gIDsVarIndex);
                }
            }

            if (clazz == long[].class) {
                final var isTopAppIndex = i - 1;
                final var isTopApp = (boolean) args[isTopAppIndex];
                return new Pair<>(isTopApp, gIDsVarIndex);
            }
        }

        return new Pair<>(false, gIDsVarIndex);
    }

    @Nullable
    private List<Integer> getRestrictedZygotePermissions(String caller) {
        final var perms = service.getRestrictedZygotePermissions(caller);
        if (perms != null && !perms.isEmpty()) {
            return perms.stream()
                    .filter(e -> Constants.GID_PAIRS.containsValue(e) || e == Constants.APP_ZYGOTE_GID)
                    .collect(Collectors.toList());
        }

        return perms;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private ProcessParams.Builder makeProcessParamsBuilder(ProcessParams params) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final var constructor = ProcessParams.Builder.class.getDeclaredConstructor(ProcessParams.class);
        ArtMethodUtils.makeExecutablePublic(constructor);
        final var newObj = constructor.newInstance(params);

        // they don't set zygotePolicyFlags in that constructor, so an override maybe required
        newObj.setZygotePolicyFlags(params.zygotePolicyFlags);

        return newObj;
    }
}
