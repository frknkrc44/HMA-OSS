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
import java.lang.invoke.MethodHandle
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class BulkHooker {
    private companion object {
        const val PARAMETER_COUNT_UNKNOWN = -1
    }

    internal val hooks = ConcurrentHashMap<String, CopyOnWriteArrayList<HookElement>>()

    internal fun isHookAvailable(clazz: String, method: String) = findHookElement(clazz, method) != null

    private fun findHookElement(clazz: String, method: String) =
        hooks[clazz]?.firstOrNull { it.methodName == method }

    private fun addHook(clazz: String, methodName: String, argumentCount: Int, impl: HookTransformer) {
        val inDisabledHooks = service?.config?.disabledHooks?.any {
            clazz == it.className &&
                    methodName == it.methodName &&
                    argumentCount == it.argumentCount
        }

        if (inDisabledHooks == true) {
            logI(ZygoteEntry.TAG) { "Disabled hook: $clazz -> $methodName($argumentCount)" }
            return
        }

        val element = HookElement(
            impl = impl,
            methodName = methodName,
            argumentCount = argumentCount,
        )

        if (applyHook(clazz, element)) {
            hooks.computeIfAbsent(clazz) { CopyOnWriteArrayList() }.add(element)
        } else {
            logI(ZygoteEntry.TAG) { "Invalid hook removed: $clazz -> $methodName($argumentCount)" }
        }
    }

    internal fun hookBefore(
        clazz: String,
        methodName: String,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount) { original, frame ->
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

    internal fun hookAfter(
        clazz: String,
        methodName: String,
        argumentCount: Int = PARAMETER_COUNT_UNKNOWN,
        hook: (methodName: String, frame: EmulatedStackFrame, returnValue: ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount) { original, frame ->
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

        try {
            hook(methodName, frame, value)
        } catch (it: Throwable) {
            logE(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on hook" }
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
        var curClazz: Class<*>?
        try {
            curClazz = Class.forName(clazz, true, loader)
        } catch (ex: ClassNotFoundException) {
            logE(ZygoteEntry.TAG, ex) { "Class $clazz not found" }
            return false
        }

        val isConstructorHook = element.methodName == CONSTRUCTOR_METHOD_NAME

        fun applyForClass(clazz: Class<*>?) {
            if (isConstructorHook) {
                Reflection.getHiddenConstructors(clazz).let { constructors ->
                    if (element.argumentCount >= 0) {
                        constructors.filter {
                            element.argumentCount == it.parameterCount
                        }.toTypedArray()
                    } else {
                        constructors
                    }
                }.firstOrNull()
            } else {
                Reflection.getHiddenExecutables(clazz).filter { executable ->
                    if (element.methodName == executable.name) {
                        if (element.argumentCount >= 0) {
                            return@filter element.argumentCount == executable.parameterCount
                        }

                        return@filter true
                    }

                    return@filter false
                }.firstOrNull()
            }?.let { executable ->
                logD(ZygoteEntry.TAG) { "Hooked constructor: $executable" }

                val memoryAddresses = Hooks.hook(
                    executable, Hooks.EntryPointType.DIRECT,
                    element.impl, Hooks.EntryPointType.DIRECT
                )

                logV(ZygoteEntry.TAG) { "Memory address map: $memoryAddresses" }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    element.memoryAddresses = memoryAddresses
                    element.method = executable
                }

                element.hookFinished = true
            }
        }

        while (
            !element.hookFinished &&
            curClazz != null &&
            curClazz.javaClass.simpleName != "Object"
        ) {
            applyForClass(curClazz)
            curClazz = curClazz.superclass
        }

        return element.hookFinished
    }

    private fun invokeExactCompat(
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

            val method = if (element.method is Method) {
                element.method as Method
            } else {
                Reflection.constructorToMethod(element.method as Constructor<*>)
            }

            value.result = method.invoke(thisObject, *args)

            ArtMethodUtils.setExecutableEntryPoint(
                element.method!!,
                element.memoryAddresses?.first!!
            )
        } else {
            Transformers.invokeExactNoChecks(original, frame)
        }
    }

    fun findAltMethod(
        clazzNames: List<String>,
        methodNames: List<String>,
        argumentCount: Int = -1,
        loader: ClassLoader? = SystemServerHook.classLoader,
    ): Executable? {
        for (clazz in clazzNames) {
            var curClazz: Class<*>?
            try {
                curClazz = Class.forName(clazz, true, loader)
            } catch (ex: ClassNotFoundException) {
                logE(ZygoteEntry.TAG, ex) { "Class $clazz not found" }
                continue
            }

            fun findMethods(clazz: Class<*>): List<Executable> {
                return Reflection.getHiddenExecutables(clazz).filter { executable ->
                    if (executable.name in methodNames) {
                        if (argumentCount >= 0) {
                            return@filter argumentCount == executable.parameterCount
                        }

                        return@filter true
                    }

                    return@filter false
                }.sortedWith { v1, v2 ->
                    v1.parameterCount.compareTo(v2.parameterCount)
                }
            }

            var methods = listOf<Executable>()

            while (
                methods.isEmpty() &&
                curClazz != null &&
                curClazz.javaClass.simpleName != "Object"
            ) {
                methods = findMethods(curClazz)
                curClazz = curClazz.superclass
            }

            return methods.firstOrNull()
        }

        logI(ZygoteEntry.TAG) { "Invalid hook detected: $clazzNames -> $methodNames($argumentCount)" }

        return null
    }
}
