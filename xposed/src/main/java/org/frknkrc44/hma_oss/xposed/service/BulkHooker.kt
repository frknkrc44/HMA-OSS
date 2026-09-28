package org.frknkrc44.hma_oss.xposed.service

import io.github.libxposed.api.XposedInterface.HookHandle
import org.frknkrc44.hma_oss.xposed.util.Logcat.logE
import org.frknkrc44.hma_oss.xposed.util.Logcat.logI
import java.lang.reflect.Executable
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class BulkHooker {
    data class HookElement(val methodName: String, val argumentCount: Int, val handle: HookHandle)

    var hooksWasCrashed = false
        private set
    internal val hooks = ConcurrentHashMap<String, CopyOnWriteArrayList<HookElement>>()

    internal fun isHookAvailable(clazz: String, method: String) =
        hooks[clazz]?.any { it.methodName == method } == true

    internal fun hookBefore(
        clazz: String, methodName: String, argumentCount: Int = -1,
        hook: (String, HookFrame, ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount, false, hook)

    internal fun hookAfter(
        clazz: String, methodName: String, argumentCount: Int = -1,
        hook: (String, HookFrame, ReturnValue) -> Unit,
    ) = addHook(clazz, methodName, argumentCount, true, hook)

    private fun addHook(
        clazz: String, methodName: String, argumentCount: Int, after: Boolean,
        callback: (String, HookFrame, ReturnValue) -> Unit,
    ) {
        if (UserService.service?.config?.disabledHooks?.any {
            it.className == clazz && it.methodName == methodName && it.argumentCount == argumentCount
        } == true) return

        val methods = resolve(clazz, methodName, argumentCount, SystemServerHook.classLoader)
        if (methods.isEmpty()) {
            logI(TAG) { "Unavailable hook: $clazz#$methodName($argumentCount)" }
            return
        }
        methods.forEach { method ->
            try {
                val handle = SystemServerHook.framework.hook(method).intercept(
                    ProtectiveHooker(method, methodName, after, callback)
                )
                hooks.computeIfAbsent(clazz) { CopyOnWriteArrayList() }
                    .add(HookElement(methodName, argumentCount, handle))
                logI(TAG) { "Installed: $method" }
            } catch (error: Throwable) {
                hooksWasCrashed = true
                logE(TAG, error) { "Hook installation failed: $method" }
            }
        }
    }

    fun findAltMethod(
        clazzNames: List<String>, methodNames: List<String>, argumentCount: Int = -1,
        loader: ClassLoader? = SystemServerHook.classLoader,
    ): Executable? {
        for (clazz in clazzNames) for (method in methodNames) {
            resolve(clazz, method, argumentCount, loader).firstOrNull()?.let { return it }
        }
        return null
    }

    private fun resolve(clazz: String, name: String, count: Int, loader: ClassLoader?): List<Executable> {
        var type: Class<*>? = try {
            Class.forName(clazz, false, loader)
        } catch (_: ClassNotFoundException) {
            return emptyList()
        }
        while (type != null) {
            val candidates: List<Executable> = if (name == "<init>") type.declaredConstructors.toList()
                else type.declaredMethods.filter { it.name == name && !it.isBridge && !Modifier.isAbstract(it.modifiers) }
            val matches = candidates.filter { count < 0 || it.parameterCount == count }
            if (matches.isNotEmpty()) return matches
            if (name == "<init>") break
            type = type.superclass
        }
        return emptyList()
    }

    private companion object { const val TAG = "HMA-LibXposed" }
}
