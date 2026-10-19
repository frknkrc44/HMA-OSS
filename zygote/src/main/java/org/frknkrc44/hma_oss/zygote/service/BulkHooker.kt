package org.frknkrc44.hma_oss.zygote.service

import android.os.Build
import com.v7878.unsafe.ArtMethodUtils
import com.v7878.unsafe.Reflection
import com.v7878.unsafe.invoke.EmulatedStackFrame
import com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX
import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.HookTransformer
import com.v7878.vmtools.Hooks
import org.frknkrc44.hma_oss.zygote.ZygoteEntry
import org.frknkrc44.hma_oss.zygote.service.UserService.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setReturnValue
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.CONSTRUCTOR_METHOD_NAME
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PARAMETER_COUNT_UNKNOWN
import java.lang.invoke.MethodHandle
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class BulkHooker {
    var hooksWasCrashed = false

    internal val hooks = ConcurrentHashMap<String, CopyOnWriteArrayList<HookElement>>()

    internal fun isHookAvailable(clazz: String, method: String) = findHookElement(clazz, method) != null

    private fun findHookElement(clazz: String, method: String) =
        hooks[clazz]?.firstOrNull { it.methodName == method }

    @PublishedApi
    internal fun addHook(clazz: String, methodName: String, argumentCount: Int, impl: HookTransformer) {
        val element = preAddHook(clazz, methodName, argumentCount, impl)

        if (element != null && applyHook(clazz, element)) {
            hooks.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(element)
        } else if (!hooksWasCrashed) {
            logI(ZygoteEntry.TAG) { "Invalid hook removed: $clazz -> $methodName($argumentCount)" }
        }
    }

    @PublishedApi
    internal fun addHookOnMethod(executable: Executable, impl: HookTransformer) {
        val methodName = executable.name
        val clazz = executable.declaringClass.name
        val argumentCount = executable.parameterCount

        val element = preAddHook(clazz, methodName, argumentCount, impl)

        if (element != null && hookExecutable(executable, element)) {
            hooks.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(element)
        } else if (!hooksWasCrashed) {
            logI(ZygoteEntry.TAG) { "Invalid hook removed: $clazz -> $methodName($argumentCount)" }
        }
    }

    private fun preAddHook(clazz: String, methodName: String, argumentCount: Int, impl: HookTransformer): HookElement? {
        val isConstructorHook = methodName == CONSTRUCTOR_METHOD_NAME
        if (isConstructorHook && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            logI(ZygoteEntry.TAG) { "Constructor hook removed for Android 12-: $clazz -> $methodName($argumentCount)" }
            return null
        }

        val inDisabledHooks = service?.config?.disabledHooks?.any {
            clazz == it.className &&
                    methodName == it.methodName &&
                    argumentCount == it.argumentCount
        }

        if (inDisabledHooks == true) {
            logI(ZygoteEntry.TAG) { "Disabled hook: $clazz -> $methodName($argumentCount)" }
            return null
        }

        return HookElement(
            impl = impl,
            methodName = methodName,
            argumentCount = argumentCount,
        )
    }

    inline fun hookBefore(
        clazzNames: List<String>,
        methodNames: List<String>,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = findAltMethod(clazzNames, methodNames, argumentCount)?.let { executable ->
        addHookOnMethod(executable) { original, frame ->
            val methodName = executable.name
            val clazz = executable.declaringClass.name

            hookBeforeCommon(clazz, methodName, original, frame, hook)
        }
    }

    inline fun hookBefore(
        clazz: String,
        methodName: String,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount) { original, frame ->
        hookBeforeCommon(clazz, methodName, original, frame, hook)
    }

    @PublishedApi
    internal inline fun hookBeforeCommon(
        clazz: String,
        methodName: String,
        original: MethodHandle,
        frame: EmulatedStackFrame,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) {
        val value = ReturnValue()

        try {
            hook(methodName, frame, value)
        } catch (it: Throwable) {
            logE(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on hook" }
        }

        if (!value.replace) {
            try {
                invokeExactCompat(clazz, methodName, original, frame, value)
            } catch (it: Throwable) {
                logD(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on original function" }
                value.throwable = it
            }
        }

        value.throwable?.let {
            ServiceUtils.clearStackTraces(it)

            throw it
        }

        if (value.replace) {
            frame.setReturnValue(value.result)
        }
    }

    inline fun hookAfter(
        clazzNames: List<String>,
        methodNames: List<String>,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        handleAfterThrows: Boolean = false,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = findAltMethod(clazzNames, methodNames, argumentCount)?.let { executable ->
        addHookOnMethod(executable) { original, frame ->
            val methodName = executable.name
            val clazz = executable.declaringClass.name

            hookAfterCommon(clazz, methodName, handleAfterThrows, original, frame, hook)
        }
    }

    inline fun hookAfter(
        clazz: String,
        methodName: String,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        handleAfterThrows: Boolean = false,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount) { original, frame ->
        hookAfterCommon(clazz, methodName, handleAfterThrows, original, frame, hook)
    }

    @PublishedApi
    internal inline fun hookAfterCommon(
        clazz: String,
        methodName: String,
        handleAfterThrows: Boolean = false,
        original: MethodHandle,
        frame: EmulatedStackFrame,
        crossinline hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) {
        val value = ReturnValue()

        try {
            invokeExactCompat(clazz, methodName, original, frame, value)
        } catch (it: Throwable) {
            logD(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on original function" }
            value.throwable = it
        }

        if (value.throwable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value.result = frame.accessor().getValue(RETURN_VALUE_IDX)
        }

        if (handleAfterThrows || value.throwable == null) {
            try {
                hook(methodName, frame, value)
            } catch (it: Throwable) {
                logE(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on hook" }
            }
        }

        value.throwable?.let {
            ServiceUtils.clearStackTraces(it)

            throw it
        }

        frame.setReturnValue(value.result)
    }

    private fun applyHook(
        clazz: String,
        element: HookElement,
        loader: ClassLoader? = SystemServerHook.classLoader,
    ): Boolean {
        // do not apply next hooks when the previous hook was crashed
        if (hooksWasCrashed) return false

        var curClazz = try {
            Class.forName(clazz, true, loader)
        } catch (ex: ClassNotFoundException) {
            logE(ZygoteEntry.TAG, ex) { "Class $clazz not found" }
            return false
        }

        while (
            !element.hookFinished &&
            curClazz != null &&
            curClazz.javaClass.simpleName != "Object"
        ) {
            val executable = resolveExecutable(curClazz, element.methodName, element.argumentCount)
            if (hookExecutable(executable, element)) return true

            curClazz = curClazz.superclass
        }

        return element.hookFinished
    }

    private fun hookExecutable(executable: Executable?, element: HookElement): Boolean {
        // do not apply next hooks when null object received or the previous hook was crashed
        if (executable == null || hooksWasCrashed) return false

        logD(ZygoteEntry.TAG) { "Hooked constructor: $executable" }

        val memoryAddresses = try {
            Hooks.hook(
                executable, Hooks.EntryPointType.DIRECT,
                element.impl, Hooks.EntryPointType.DIRECT
            )
        } catch (e: Throwable) {
            logE(ZygoteEntry.TAG, e) {
                "Hook ${executable.declaringClass} -> ${element.methodName}(${element.argumentCount}) crashed!"
            }

            hooksWasCrashed = true

            return false
        }

        logV(ZygoteEntry.TAG) { "Memory address map: $memoryAddresses" }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            element.memoryAddresses = memoryAddresses
            element.method = executable
        }

        element.hookFinished = true
        return true
    }

    @PublishedApi
    internal fun invokeExactCompat(
        clazz: String,
        methodName: String,
        original: MethodHandle,
        frame: EmulatedStackFrame,
        value: ReturnValue,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val element = findHookElement(clazz, methodName)!!

            ArtMethodUtils.setExecutableEntryPoint(
                element.method!!,
                element.memoryAddresses?.second!!
            )

            val thisObject = frame.getArgument(0)
            val args = frame.dumpArgs(true)

            // TODO: DO NOT USE ... as Constructor<*>, IT BREAKS TANGO!!!
            value.result = (element.method as Method).invoke(thisObject, *args)

            ArtMethodUtils.setExecutableEntryPoint(
                element.method!!,
                element.memoryAddresses?.first!!
            )
        } else {
            Transformers.invokeExactNoChecks(original, frame)
        }
    }

    @PublishedApi
    internal fun findAltMethod(
        clazzNames: List<String>,
        methodNames: List<String>,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        loader: ClassLoader? = SystemServerHook.classLoader,
    ): Executable? {
        for (clazz in clazzNames) {
            var curClazz = try {
                Class.forName(clazz, true, loader)
            } catch (ex: ClassNotFoundException) {
                logE(ZygoteEntry.TAG, ex) { "Class $clazz not found" }
                continue
            }

            var methods = listOf<Executable>()
            while (
                methods.isEmpty() &&
                curClazz != null &&
                curClazz.javaClass.simpleName != "Object"
            ) {
                methods = methodNames.mapNotNull {
                    resolveExecutable(curClazz, it, argumentCount)
                }
                curClazz = curClazz.superclass
            }

            return methods.firstOrNull() ?: continue
        }

        return null
    }

    private fun resolveExecutable(
        clazz: Class<*>?,
        methodName: String,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
    ): Executable? {
        val isConstructorHook = methodName == CONSTRUCTOR_METHOD_NAME

        return if (isConstructorHook) {
            Reflection.getHiddenConstructors(clazz).firstOrNull {
                return@firstOrNull argumentCount < 0 || argumentCount == it.parameterCount
            }
        } else {
            Reflection.getHiddenExecutables(clazz).firstOrNull {
                return@firstOrNull methodName == it.name &&
                        (argumentCount < 0 || argumentCount == it.parameterCount)
            }
        }
    }
}
