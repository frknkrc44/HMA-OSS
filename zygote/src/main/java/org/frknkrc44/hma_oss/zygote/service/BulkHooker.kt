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
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils
import org.frknkrc44.hma_oss.zygote.util.ZLUtils
import java.lang.invoke.MethodHandle
import java.lang.reflect.Executable
import java.lang.reflect.Method

class BulkHooker private constructor() {
    companion object {
        val instance: BulkHooker by lazy { BulkHooker() }
        const val PARAMETER_COUNT_UNKNOWN = -1
    }

    internal val hooks: MutableMap<String, MutableList<HookElement>> = HashMap()

    private fun addHook(clazz: String, methodName: String, hookOnce: Boolean, paramCount: Int, impl: HookTransformer) {
        val inDisabledHooks = HMAService.instance?.config?.disabledHooks?.any {
            clazz == it.className &&
                    methodName == it.methodName &&
                    paramCount == it.argumentCount
        }

        if (inDisabledHooks == true) {
            logI(ZygoteEntry.TAG) { "Disabled hook: $clazz -> $methodName($paramCount)" }
            return
        }

        val element = HookElement(
            impl = impl,
            methodName = methodName,
            hookOnce = hookOnce,
            paramCount = paramCount,
        )

        if (applyHook(clazz, element)) {
            if (clazz !in hooks) {
                hooks[clazz] = mutableListOf()
            }

            hooks[clazz]!!.add(element)
        } else {
            logI(ZygoteEntry.TAG) { "Invalid hook removed: $clazz -> $methodName($paramCount)" }
        }
    }

    internal fun hookBefore(
        clazz: String,
        methodName: String,
        hookOnce: Boolean = true,
        paramCount: Int = PARAMETER_COUNT_UNKNOWN,
        hook: (param: HookParam) -> Unit,
    ) {
        addHook(clazz, methodName, hookOnce, paramCount) { original, frame ->
            val value = ReturnValue()

            try {
                hook(HookParam(clazz, original, frame, methodName, value))
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
                ZLUtils.setReturnValue(frame, value.result)
            }
        }
    }

    internal fun hookAfter(
        clazz: String,
        methodName: String,
        hookOnce: Boolean = true,
        paramCount: Int = PARAMETER_COUNT_UNKNOWN,
        hook: (param: HookParam) -> Unit,
    ) {
        addHook(clazz, methodName, hookOnce, paramCount) { original, frame ->
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
                hook(HookParam(clazz, original, frame, methodName, value))
            } catch (it: Throwable) {
                logE(ZygoteEntry.TAG, it) { it.message ?: "Unknown error on hook" }
            }

            value.throwable?.let {
                ServiceUtils.clearStackTraces(it)

                throw it
            }

            ZLUtils.setReturnValue(frame, value.result)
        }
    }

    internal fun applyHook(
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

        fun applyForClass(clazz: Class<*>?) {
            val executables = Reflection.getHiddenExecutables(clazz).filter { executable ->
                element.methodName == executable.name &&
                    (element.paramCount in listOf(PARAMETER_COUNT_UNKNOWN, executable.parameterCount))
            }.sortedWith { v1, v2 ->
                v1.parameterCount.compareTo(v2.parameterCount)
            }

            for (executable in executables) {
                if (!element.hookFinished) {
                    logD(ZygoteEntry.TAG) { "Hooked: $executable" }

                    val memoryAddresses = Hooks.hook(
                        executable, Hooks.EntryPointType.DIRECT,
                        element.impl, Hooks.EntryPointType.DIRECT
                    )

                    logV(ZygoteEntry.TAG) { "Memory address map: $memoryAddresses" }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        element.memoryAddresses = memoryAddresses
                        element.method = executable
                    }

                    element.applyCount++

                    if (element.hookOnce) {
                        element.hookFinished = true
                        break
                    }
                }
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

        if (!element.hookOnce && element.applyCount >= 1) {
            element.hookFinished = true
        }

        return element.hookFinished
    }

    fun invokeExactCompat(clazz: String, methodName: String, original: MethodHandle, frame: EmulatedStackFrame, value: ReturnValue) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val element = findHookElement(clazz, methodName)!!

            ArtMethodUtils.setExecutableEntryPoint(
                element.method!!,
                element.memoryAddresses?.second!!
            )

            val thisObject = ZLUtils.getArgument(frame, 0)
            val args = ZLUtils.dumpArgs(frame, true)

            value.result = (element.method as Method).invoke(thisObject, *args)

            ArtMethodUtils.setExecutableEntryPoint(
                element.method!!,
                element.memoryAddresses?.first!!
            )
        } else {
            Transformers.invokeExactNoChecks(original, frame)
        }
    }

    fun findHookElement(clazz: String, methodName: String): HookElement? {
        hooks[clazz]?.forEach { element ->
            if (element.methodName == methodName) {
                return element
            }
        }

        return null
    }

    fun findAltMethod(
        clazzNames: List<String>,
        methodNames: List<String>,
        paramCount: Int = -1,
        loader: ClassLoader? = SystemServerHook.classLoader,
    ): Executable? {
        for (clazz in clazzNames) {
            var curClazz: Class<*>?
            try {
                curClazz = Class.forName(clazz, true, loader)
            } catch (ex: ClassNotFoundException) {
                logE(ZygoteEntry.TAG, ex) { "Class $clazz not found" }
                return null
            }

            fun findMethods(clazz: Class<*>): List<Executable> {
                return Reflection.getHiddenExecutables(clazz).filter { executable ->
                    executable.name in methodNames &&
                            (paramCount in listOf(PARAMETER_COUNT_UNKNOWN, executable.parameterCount))
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

        logI(ZygoteEntry.TAG) { "Invalid hook detected: $clazzNames -> $methodNames($paramCount)" }

        return null
    }
}
