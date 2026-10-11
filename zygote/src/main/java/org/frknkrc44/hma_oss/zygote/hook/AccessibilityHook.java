package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getScopedCaller;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getReturnType;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACCESSIBILITY_SERVICE_CLASS;
import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.Settings;

import com.v7878.unsafe.invoke.EmulatedStackFrame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import icu.nullptr.hidemyapplist.common.Constants;
import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset;
import org.frknkrc44.hma_oss.zygote.service.ReturnValue;

public class AccessibilityHook extends ABaseFrameworkHook {

    public AccessibilityHook() {
        super("AccessibilityHook");
    }

    @Override
    public void load() {
        super.load();

        service.hooker.hookBefore(
                ACCESSIBILITY_SERVICE_CLASS,
                "getEnabledAccessibilityServiceList",
                (methodName, frame, returnValue) -> {
                    final var callingApps = getCallingApps(service.pms);
                    if (callingApps.length < 1) return;

                    final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
                    if (caller != null) {
                        logD(TAG, null, () -> String.format(
                                "@%s returning empty list for %s",
                                methodName,
                                caller
                        ));

                        final var returnedList = new ArrayList<AccessibilityServiceInfo>();
                        returnValue.setResult(
                                getReturnType(frame).getSimpleName().startsWith("Parcel")
                                        ? new ParceledListSlice<>(returnedList)
                                        : returnedList
                        );
                    }
                }
        );

        service.hooker.hookBefore(
                ACCESSIBILITY_SERVICE_CLASS,
                "addClient",
                (methodName, frame, returnValue) -> {
                    final var callingApps = getCallingApps(service.pms);
                    if (callingApps.length < 1) return;

                    final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
                    if (caller != null) {
                        returnValue.setResult(0L);
                    }
                }
        );

        service.hooker.hookAfter(
                ACCESSIBILITY_SERVICE_CLASS,
                "getEnabledAccessibilityServiceList",
                (methodName, frame, returnValue) -> filterAccessibilityServices(methodName, frame, returnValue, true)
        );

        service.hooker.hookAfter(
                ACCESSIBILITY_SERVICE_CLASS,
                "getInstalledAccessibilityServiceList",
                (methodName, frame, returnValue) -> filterAccessibilityServices(methodName, frame, returnValue, false)
        );
    }

    @SuppressWarnings("unchecked")
    private void filterAccessibilityServices(
            String methodName,
            EmulatedStackFrame frame,
            ReturnValue returnValue,
            boolean isEnabledList
    ) throws RemoteException {
        final var currentResult = returnValue.getResult();
        if (currentResult == null) return;

        final var callingApps = getCallingApps(service.pms);
        if (callingApps.length < 1) return;

        final var caller = getScopedCaller(callingApps, service::isHookEnabled);
        if (caller == null) return;

        final var returnType = getReturnType(frame);
        final boolean isParceled = returnType.getSimpleName().startsWith("Parcel");

        final List<AccessibilityServiceInfo> rawList = (isParceled && currentResult instanceof ParceledListSlice<?> slice)
                ? (List<AccessibilityServiceInfo>) slice.getList()
                : (currentResult instanceof List<?> list ? (List<AccessibilityServiceInfo>) list : null);
        if (rawList == null || rawList.isEmpty()) return;

        final var callingUserId = getUserFromCallingUid(Binder.getCallingUid());

        // 消除手动服务列表与黑名单过滤的冲突
        final var manualSetting = isEnabledList
                ? service.getSpoofedSetting(caller, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, Constants.SETTINGS_SECURE)
                : null;
        final Set<ComponentName> manualComponents = new HashSet<>();
        final Set<String> manualStrings = new HashSet<>();
        if (manualSetting != null && manualSetting.getValue() != null && !manualSetting.getValue().isEmpty()) {
            for (var item : manualSetting.getValue().split(":")) {
                final var trimmed = item.trim();
                if (trimmed.isEmpty()) continue;
                manualStrings.add(trimmed);
                final var comp = ComponentName.unflattenFromString(trimmed);
                if (comp != null) {
                    manualComponents.add(comp);
                    manualStrings.add(comp.getPackageName());
                } else if (trimmed.contains("/")) {
                    manualStrings.add(trimmed.split("/")[0]);
                }
            }
        }

        final var filteredList = new ArrayList<AccessibilityServiceInfo>();
        int filterCount = 0;

        for (var info : rawList) {
            if (info == null) continue;
            final var si = info.getResolveInfo() != null ? info.getResolveInfo().serviceInfo : null;
            final var pkg = si != null ? si.packageName : (info.getId() != null ? info.getId().split("/")[0] : null);
            final var comp = si != null && si.packageName != null && si.name != null
                    ? new ComponentName(si.packageName, si.name)
                    : (info.getId() != null ? ComponentName.unflattenFromString(info.getId()) : null);

            // 黑名单动态过滤
            if (pkg != null && service.shouldHide(caller, pkg, callingUserId)) {
                filterCount++;
                logD(TAG, null, () -> String.format("@%s filtered blacklisted service %s for %s", methodName, pkg, caller));
                continue;
            }

            // 手动指定服务列表过滤（取交集）
            if (manualSetting != null) {
                final var id = info.getId();
                final boolean allowed = (comp != null && manualComponents.contains(comp))
                        || (id != null && manualStrings.contains(id))
                        || (pkg != null && manualStrings.contains(pkg));
                if (!allowed) {
                    filterCount++;
                    logD(TAG, null, () -> String.format("@%s filtered service %s (not in manual list) for %s", methodName, pkg, caller));
                    continue;
                }
            }

            filteredList.add(info);
        }

        if (filterCount > 0) {
            returnValue.setResult(isParceled ? new ParceledListSlice<>(filteredList) : filteredList);
            service.increaseSettingsFilterCount(caller, filterCount);
            final int finalFilterCount = filterCount;
            logD(TAG, null, () -> String.format("@%s filtered %d services for %s, %d retained",
                    methodName, finalFilterCount, caller, filteredList.size()));
        }
    }

    private boolean callerIsSpoofed(String caller) {
        final var presets = service.getEnabledSettingsPresets(caller);
        return presets != null && presets.contains(AccessibilityPreset.NAME);
    }
}

