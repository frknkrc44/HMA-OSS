package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.ContextUtils.getApplication;
import static org.frknkrc44.hma_oss.zygote.util.ContextUtils.getPackageManager;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logW;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.binderLocalScope;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.binderLocalScopeNoThrow;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getScopedCaller;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getReturnType;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GBOARD_CLASS_NAME;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.GBOARD_PACKAGE_NAME;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.IMM_IMPL_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.IMM_SERVICE_CLASS;
import static icu.nullptr.hidemyapplist.common.Utils.getCallingUser;
import static icu.nullptr.hidemyapplist.common.Utils.getPackageUidCompat;
import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.inputmethod.InputMethodInfoSafeList;
import com.android.internal.inputmethod.InputMethodSubtypeSafeList;
import com.v7878.unsafe.invoke.EmulatedStackFrame;

import org.frknkrc44.hma_oss.zygote.service.ReturnValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import icu.nullptr.hidemyapplist.common.Constants;
import icu.nullptr.hidemyapplist.common.settings_presets.InputMethodPreset;

public class ImmHook extends ABaseFrameworkHook {

    public ImmHook() {
        super("ImmHook");
    }

    private final InputMethodInfo defaultReturn = new InputMethodInfo(
            GBOARD_PACKAGE_NAME,
            GBOARD_CLASS_NAME,
            "Gboard",
            null
    );

    // OEMs (especially Samsung and Xiaomi) messes up whole framework code,
    // so nothing left except messing up this code
    @Override
    public void load() {
        super.load();

        final var currentIMMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                List.of("getCurrentInputMethodInfoAsUser")
        );
        service.hooker.hookBefore(
                currentIMMethod,
                (methodName, frame, returnValue) -> {
                    final var callingApps = getCallingApps(service.pms);

                    final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
                    if (caller == null) return;

                    logD(TAG, null, () -> String.format(
                            "@%s: spoofed input method for %s",
                            methodName, caller
                    ));

                    final var fakeIMInfo = getFakeInputMethodInfo(caller);
                    // noinspection all - we have no potential NPEs for this method
                    final var handle = (int) getArgument(frame, 1);
                    if (isIMNotExists(fakeIMInfo.getPackageName(), handle)) {
                        warnNotInstalledKeyboard(methodName, fakeIMInfo.getPackageName());
                    }

                    returnValue.setResult(fakeIMInfo);
                    service.increaseSettingsFilterCount(caller);
                }
        );

        final var getIMMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS),
                List.of("getInputMethodList", "getInputMethodListInternal")
        );
        service.hooker.hookAfter(
                getIMMethod,
                (methodName, frame, returnValue) -> {
                    logD(TAG, null, () -> "@" + methodName + ": hook init");

                    final var result = returnValue.getResult();
                    if (result == null) return;

                    final var args = dumpArgs(frame, true);
                    logD(TAG, null, () -> String.format(
                            "@%s: Result: %s Args: %s",
                            methodName, result, Arrays.toString(args)
                    ));

                    int callingUid = -1;
                    for (var item : args) {
                        if (item instanceof Integer in) {
                            if (in > 999) {
                                callingUid = in;
                                break;
                            }
                        }
                    }

                    if (callingUid < 0) {
                        callingUid = Binder.getCallingUid();
                    }

                    if (service.config.getDetailLog()) {
                        int finalCallingUid = callingUid;
                        logD(TAG, null, () -> String.format(Locale.US,
                                "@%s:  Caller ID: %d",
                                methodName, finalCallingUid
                        ));
                    }

                    if (getReturnType(frame) == InputMethodInfoSafeList.class) {
                        final var toExtract = (InputMethodInfoSafeList) result;
                        final var extracted = InputMethodInfoSafeList.extractFrom(toExtract);
                        final var newList = calculateReturnedInputMethodList(callingUid, extracted);
                        returnValue.setResult(InputMethodInfoSafeList.create(newList));
                    } else {
                        // noinspection unchecked
                        returnValue.setResult(calculateReturnedInputMethodList(
                                callingUid, (List<InputMethodInfo>) result
                        ));
                    }
                }
        );

        final var getEnabledIMMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS),
                List.of("getEnabledInputMethodList", "getEnabledInputMethodListInternal")
        );
        service.hooker.hookBefore(
                getEnabledIMMethod,
                (methodName, frame, returnValue) -> {
                    final var callingApps = getCallingApps(service.pms);

                    final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
                    if (caller == null) return;

                    logD(TAG, null, () -> String.format(
                            "@%s: spoofed input method for %s",
                            methodName, caller
                    ));

                    final var fakeIMInfo = getFakeInputMethodInfo(caller);
                    if (isIMNotExists(fakeIMInfo.getPackageName(), getCallingUser())) {
                        warnNotInstalledKeyboard(methodName, fakeIMInfo.getPackageName());
                    }

                    final var list = List.of(fakeIMInfo);
                    final var returnType = getReturnType(frame);
                    if (returnType == InputMethodInfoSafeList.class) {
                        returnValue.setResult(InputMethodInfoSafeList.create(list));
                    } else {
                        returnValue.setResult(list);
                    }

                    service.increaseSettingsFilterCount(caller);
                }
        );

        final var getCurrentIMSTMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                List.of("getCurrentInputMethodSubtype")
        );
        service.hooker.hookBefore(
                getCurrentIMSTMethod,
                (methodName, frame, returnValue) -> subtypeHook(methodName, returnValue)
        );

        final var getLastIMSTMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                List.of("getLastInputMethodSubtype")
        );
        service.hooker.hookBefore(
                getLastIMSTMethod,
                (methodName, frame, returnValue) -> subtypeHook(methodName, returnValue)
        );

        final var getEnabledIMSLMethod = (Method) service.hooker.findAltMethod(
                List.of(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                List.of("getEnabledInputMethodSubtypeListInternal", "getEnabledInputMethodSubtypeList")
        );
        service.hooker.hookBefore(getEnabledIMSLMethod, this::subtypeListHook);
    }

    @NonNull
    private ComponentName getFakeInputMethodComponent(String caller) {
        final var defaultIM = service.getSpoofedSetting(
                caller,
                Settings.Secure.DEFAULT_INPUT_METHOD,
                Constants.SETTINGS_SECURE
        );

        if (defaultIM != null && defaultIM.getValue() != null) {
            final var unflatten = ComponentName.unflattenFromString(defaultIM.getValue());
            if (unflatten != null) {
                return unflatten;
            }
        }

        return new ComponentName(GBOARD_PACKAGE_NAME, GBOARD_CLASS_NAME);
    }

    @NonNull
    private InputMethodInfo getFakeInputMethodInfo(String caller) {
        final var component = getFakeInputMethodComponent(caller);

        var imInfo = resolveIMInfo(component.getPackageName());
        if (imInfo == null) {
            try {
                final var packageManager = getPackageManager();
                final var appInfo = packageManager.getApplicationInfo(component.getPackageName(), 0);

                imInfo = new InputMethodInfo(
                        component.getPackageName(),
                        component.getClassName(),
                        appInfo.loadLabel(packageManager),
                        null
                );
            } catch (Throwable ignore) {
                if (GBOARD_PACKAGE_NAME.equals(component.getPackageName())) {
                    imInfo = defaultReturn;
                } else {
                    imInfo = resolveIMInfo(GBOARD_PACKAGE_NAME);
                    if (imInfo == null) {
                        imInfo = defaultReturn;
                    }
                }
            }
        }

        return imInfo;
    }

    @Nullable
    private InputMethodInfo resolveIMInfo(String packageName) {
        return binderLocalScopeNoThrow(() -> {
            final var imManager = (InputMethodManager)
                    getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);

            for (var im : imManager.getInputMethodList()) {
                if (packageName.equals(im.getPackageName())) {
                    return im;
                }
            }

            warnNotInstalledKeyboard("resolveIMInfo", packageName);
            return null;
        });
    }

    private List<InputMethodInfo> calculateReturnedInputMethodList(int callingUid, List<InputMethodInfo> inList) throws RemoteException {
        logV(TAG, null, () -> "@getInputMethodList*calculator: " + callingUid + " - Current: " + inList);

        final var callingApps = getCallingApps(service.pms, callingUid);
        final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
        if (caller == null) return inList;

        logD(TAG, null, () -> "@getInputMethodList: spoofed input method for " + caller);

        final var callingUserId = getUserFromCallingUid(callingUid);

        final var calculatedList = inList.stream()
                .filter(e -> !service.shouldHide(caller, e.getPackageName(), callingUserId))
                .collect(Collectors.toList());

        logV(TAG, null, () -> "@getInputMethodList*calculator: " + callingUid + " - Calculated: " + calculatedList);

        final var fakeIMInfo = getFakeInputMethodInfo(caller);
        final var imNotExists = !isIMNotExists(fakeIMInfo.getPackageName(), callingUserId);
        final var calcListHasIM = calculatedList.stream()
                .anyMatch(e -> e.getPackageName().equals(fakeIMInfo.getPackageName()));

        if (imNotExists) {
            warnNotInstalledKeyboard("getInputMethodList*calculator", fakeIMInfo.getPackageName());
        }

        if (!calcListHasIM) {
            calculatedList.add(fakeIMInfo);
            calculatedList.sort(Comparator.comparing(InputMethodInfo::getPackageName));
        }

        return calculatedList;
    }

    private void subtypeHook(String methodName, ReturnValue returnValue) throws RemoteException {
        final var callingApps = getCallingApps(service.pms);

        final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
        if (caller == null) return;

        logD(TAG, null, () -> String.format(
                "@%s: spoofed input method subtype for %s",
                methodName, caller
        ));

        // TODO: Find a method to get exact value for spoofed input method
        returnValue.setResult(null);
        service.increaseSettingsFilterCount(caller);
    }

    private void subtypeListHook(String methodName, EmulatedStackFrame frame, ReturnValue returnValue) throws RemoteException {
        final var callingApps = getCallingApps(service.pms);

        final var caller = getScopedCaller(callingApps, this::callerIsSpoofed);
        if (caller == null) return;

        logD(TAG, null, () -> String.format(
                "@%s: spoofed input method subtype for %s",
                methodName, caller
        ));

        final var list = new ArrayList<InputMethodSubtype>();
        final var returnType = getReturnType(frame);

        if (returnType == InputMethodSubtypeSafeList.class) {
             returnValue.setResult(InputMethodSubtypeSafeList.create(list));
        } else {
            returnValue.setResult(list);
        }

        service.increaseSettingsFilterCount(caller);
    }

    private boolean isIMNotExists(String packageName, int userId) throws RemoteException {
        if (service.systemApps.contains(packageName)) return false;

        return binderLocalScope(() ->
                getPackageUidCompat(service.pms, packageName, PackageManager.MATCH_ALL, userId) < 0);
    }

    private void warnNotInstalledKeyboard(String methodName, String packageName) {
        logW(TAG, null, () -> String.format(
                "@%s: PROBABLY spoofing for a not installed/enabled keyboard, please install " +
                        "and enable %s or spoof for another keyboard by using settings templates to " +
                        "reduce detections. Do not care this message if you are sure the keyboard " +
                        "is installed correctly.",
                methodName, packageName
        ));
    }

    private boolean callerIsSpoofed(String caller) {
        final var presets = service.getEnabledSettingsPresets(caller);
        return presets != null && presets.contains(InputMethodPreset.NAME);
    }
}
