package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getScopedCaller;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getReturnType;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACCESSIBILITY_SERVICE_CLASS;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ParceledListSlice;

import java.util.ArrayList;

import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset;

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
    }

    private boolean callerIsSpoofed(String caller) {
        final var presets = service.getEnabledSettingsPresets(caller);
        return presets != null && presets.contains(AccessibilityPreset.NAME);
    }
}
