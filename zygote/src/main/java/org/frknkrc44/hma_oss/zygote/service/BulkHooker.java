package org.frknkrc44.hma_oss.zygote.service;

import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;
import static org.frknkrc44.hma_oss.zygote.service.UserService.service;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.PARAMETER_COUNT_UNKNOWN;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONSTRUCTOR_METHOD_NAME;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.v7878.unsafe.ArtMethodUtils;
import com.v7878.unsafe.Reflection;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.HookTransformer;
import com.v7878.vmtools.Hooks;

import org.frknkrc44.hma_oss.zygote.ZygoteEntry;
import org.frknkrc44.hma_oss.zygote.callback.HookCallback;
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils;
import org.frknkrc44.hma_oss.zygote.util.ZLUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BulkHooker {
    boolean hooksWasCrashed = false;

    final ConcurrentHashMap<String, CopyOnWriteArrayList<HookElement>> hooks = new ConcurrentHashMap<>();

    @SuppressWarnings("DataFlowIssue")
    public boolean isHookAvailable(String clazz, String method) {
        if (!hooks.containsKey(clazz)) return false;

        for (var item : hooks.get(clazz)) {
            if (method.equals(item.methodName)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    HookElement findHookElement(String clazz, String method) {
        if (!hooks.containsKey(clazz)) return null;

        for (var item : hooks.get(clazz)) {
            if (method.equals(item.methodName)) {
                return item;
            }
        }

        return null;
    }

    private void addHook(String clazz, String methodName, int argumentCount, HookTransformer impl) {
        final var isConstructorHook = CONSTRUCTOR_METHOD_NAME.equals(methodName);
        if (isConstructorHook && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            logI(ZygoteEntry.TAG, null, () -> "Constructor hook removed for Android 12-: " + clazz + " -> " + methodName + "(" + argumentCount + ")");

            return;
        }

        final var inDisabledHooks = service.config.getDisabledHooks().stream()
                .anyMatch(e ->
                        e.getClassName().equals(clazz) &&
                                e.getMethodName().equals(methodName)
                );

        if (inDisabledHooks) {
            logI(ZygoteEntry.TAG, null, () -> "Disabled hook: " + clazz + " -> " + methodName + "(" + argumentCount + ")");
            return;
        }

        final var element = new HookElement(impl, methodName, argumentCount);
        if (applyHook(clazz, element)) {
            hooks.computeIfAbsent(clazz, key -> new CopyOnWriteArrayList<>()).add(element);
        } else if (!hooksWasCrashed) {
            logI(ZygoteEntry.TAG, null, () -> "Invalid hook: " + clazz + " -> " + methodName + "(" + argumentCount + ")");
        }
    }

    public void addHookOnMethod(Method method, HookTransformer impl) {
        final var clazz = method.getDeclaringClass().getName();
        final var methodName = method.getName();
        final var argumentCount = method.getParameterCount() + 1; // add thisObject

        final var inDisabledHooks = service.config.getDisabledHooks().stream()
                .anyMatch(e ->
                        e.getClassName().equals(clazz) &&
                                e.getMethodName().equals(methodName)
                );

        if (inDisabledHooks) {
            logI(ZygoteEntry.TAG, null, () -> "Disabled hook: " + clazz + " -> " + methodName + "(" + argumentCount + ")");
            return;
        }

        final var element = new HookElement(impl, methodName, argumentCount);
        if (applyHookInternal(clazz, method, element)) {
            hooks.computeIfAbsent(clazz, key -> new CopyOnWriteArrayList<>()).add(element);
        } else if (!hooksWasCrashed) {
            logI(ZygoteEntry.TAG, null, () -> "Invalid hook: " + clazz + " -> " + methodName + "(" + argumentCount + ")");
        }
    }

    public void hookBefore(@Nullable Method method, @NonNull HookCallback hook) {
        if (method == null) return;

        final var clazz = method.getDeclaringClass().getName();
        final var methodName = method.getName();

        addHookOnMethod(method, hookBeforeCommon(clazz, methodName, hook));
    }

    public void hookBefore(String clazz, String methodName, HookCallback hook) {
        hookBefore(clazz, methodName, PARAMETER_COUNT_UNKNOWN, hook);
    }

    public void hookBefore(String clazz, String methodName, int argumentCount, HookCallback hook) {
        addHook(clazz, methodName, argumentCount, hookBeforeCommon(clazz, methodName, hook));
    }

    public void hookAfter(@Nullable Method method, @NonNull HookCallback hook) {
        hookAfter(method, false, hook);
    }

    public void hookAfter(@Nullable Method method, boolean handleAfterThrows, @NonNull HookCallback hook) {
        if (method == null) return;

        final var clazz = method.getDeclaringClass().getName();
        final var methodName = method.getName();

        addHookOnMethod(method, hookAfterCommon(clazz, methodName, handleAfterThrows, hook));
    }

    public void hookAfter(String clazz, String methodName, HookCallback hook) {
        hookAfter(clazz, methodName, PARAMETER_COUNT_UNKNOWN, false, hook);
    }

    public void hookAfter(String clazz, String methodName, int argumentCount, boolean handleAfterThrows, HookCallback hook) {
        addHook(clazz, methodName, argumentCount, hookAfterCommon(clazz, methodName, handleAfterThrows, hook));
    }

    private HookTransformer hookBeforeCommon(String clazz, String methodName, HookCallback hook) {
        return (original, frame) -> {
            final var value = new ReturnValue(null);

            try {
                hook.accept(methodName, frame, value);
            } catch (Throwable e) {
                logE(ZygoteEntry.TAG, e, () -> "An error occurred on hook");
            }

            if (!value.isReplaced()) {
                try {
                    invokeExactCompat(clazz, methodName, original, frame, value);
                } catch (Throwable e) {
                    logD(ZygoteEntry.TAG, e, () -> "An error occurred on original function");
                    value.throwable = e;
                }
            }

            if (value.throwable != null) {
                ServiceUtils.clearStackTraces(value.throwable);

                throw value.throwable;
            }

            if (value.isReplaced()) {
                ZLUtils.setReturnValue(frame, value.getResult());
            }
        };
    }

    private HookTransformer hookAfterCommon(String clazz, String methodName, boolean handleAfterThrows, HookCallback hook) {
        return (original, frame) -> {
            final var value = new ReturnValue(null);

            try {
                invokeExactCompat(clazz, methodName, original, frame, value);
            } catch (Throwable e) {
                logD(ZygoteEntry.TAG, e, () -> "An error occurred on original function");
                value.throwable = e;
            }

            if (value.throwable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                value.setResult(frame.accessor().getValue(RETURN_VALUE_IDX));
            }

            if (handleAfterThrows || value.throwable == null) {
                try {
                    hook.accept(methodName, frame, value);
                } catch (Throwable e) {
                    logE(ZygoteEntry.TAG, e, () -> "An error occurred on hook");
                }
            }

            if (value.throwable != null) {
                ServiceUtils.clearStackTraces(value.throwable);

                throw value.throwable;
            }

            ZLUtils.setReturnValue(frame, value.getResult());
        };
    }

    private boolean applyHook(String clazz, HookElement element) {
        // do not apply next hooks when the previous one was crashed
        if (hooksWasCrashed) {
            return false;
        }

        Class<?> currentClass;
        try {
            currentClass = Class.forName(clazz, true, ZygoteEntry.classLoader);
        } catch (ClassNotFoundException e) {
            logE(ZygoteEntry.TAG, e, () -> "Class " + clazz + " not found");

            return false;
        }

        while (currentClass != null && currentClass != Object.class) {
            final var executable = resolveExecutable(currentClass, element.methodName, element.argumentCount);

            if (applyHookInternal(clazz, executable, element)) break;

            currentClass = currentClass.getSuperclass();
        }

        return element.hookFinished;
    }

    private boolean applyHookInternal(String clazz, Executable executable, HookElement element) {
        if (executable != null) {
            Pair<Long, Long> memoryAddresses;

            try {
                memoryAddresses = Hooks.hook(
                        executable, Hooks.EntryPointType.DIRECT,
                        element.impl, Hooks.EntryPointType.DIRECT
                );

                logD(ZygoteEntry.TAG, null, () -> "Hooked on " + clazz + " -> " + element.methodName + "(" + element.argumentCount + ")");
            } catch (Throwable e) {
                logE(ZygoteEntry.TAG, e, () -> "Hook " + clazz + " -> " + element.methodName + "(" + element.argumentCount + ") crashed!");

                hooksWasCrashed = true;

                return false;
            }

            logV(ZygoteEntry.TAG, null, () -> "Memory address map: " + memoryAddresses);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                element.memoryAddresses = memoryAddresses;
                element.executable = executable;
            }

            element.hookFinished = true;
        }

        return element.hookFinished;
    }

    private void invokeExactCompat(
            String clazz,
            String methodName,
            MethodHandle original,
            EmulatedStackFrame frame,
            ReturnValue value
    ) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Transformers.invokeExactNoChecks(original, frame);
        } else {
            final var element = findHookElement(clazz, methodName);
            assert element != null && element.executable != null && element.memoryAddresses != null;

            ArtMethodUtils.setExecutableEntryPoint(
                    element.executable,
                    element.memoryAddresses.second
            );

            final var thisObject = ZLUtils.getArgument(frame, 0);
            final var args = ZLUtils.dumpArgs(frame, true);

            // TODO: DO NOT USE ... as Constructor<*>, IT BREAKS TANGO!!!
            value.setResult(((Method) element.executable).invoke(thisObject, args));

            ArtMethodUtils.setExecutableEntryPoint(
                    element.executable,
                    element.memoryAddresses.first
            );
        }
    }

    @Nullable
    public Executable findAltMethod(List<String> clazzNames, List<String> methodNames) {
        return findAltMethod(clazzNames, methodNames, PARAMETER_COUNT_UNKNOWN);
    }

    @Nullable
    public Executable findAltMethod(
            List<String> clazzNames,
            List<String> methodNames,
            int argumentCount
    ) {
        for (var clazz : clazzNames) {
            Class<?> currentClass;
            try {
                currentClass = Class.forName(clazz, true, ZygoteEntry.classLoader);
            } catch (ClassNotFoundException e) {
                logE(ZygoteEntry.TAG, e, () -> "Class " + clazz + " not found");

                continue;
            }

            while (currentClass != null && currentClass != Object.class) {
                for (var methodName : methodNames) {
                    final var executable = resolveExecutable(currentClass, methodName, argumentCount);

                    if (executable != null) {
                        return executable;
                    }
                }

                currentClass = currentClass.getSuperclass();
            }
        }

        return null;
    }

    @Nullable
    private Executable resolveExecutable(Class<?> clazz, String methodName, int argumentCount) {
        final var isConstructorHook = CONSTRUCTOR_METHOD_NAME.equals(methodName);
        if (isConstructorHook) {
            for (var executable : Reflection.getHiddenConstructors(clazz)) {
                if (argumentCount >= 0) {
                    if (executable.getParameterCount() == argumentCount) {
                        return executable;
                    }

                    continue;
                }

                return executable;
            }
        } else {
            for (var executable : Reflection.getHiddenExecutables(clazz)) {
                if (methodName.equals(executable.getName())) {
                    if (argumentCount >= 0) {
                        if (executable.getParameterCount() == argumentCount) {
                            return executable;
                        }

                        continue;
                    }

                    return executable;
                }
            }
        }

        return null;
    }
}
