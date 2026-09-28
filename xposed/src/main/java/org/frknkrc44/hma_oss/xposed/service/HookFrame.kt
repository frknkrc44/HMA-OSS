package org.frknkrc44.hma_oss.xposed.service

import io.github.libxposed.api.XposedInterface.Chain
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Matches VMTools' argument layout without carrying any VMTools runtime dependency. */
class HookFrame(val executable: Executable, receiver: Any?, arguments: List<Any?>) {
    private val receiverCount = if (Modifier.isStatic(executable.modifiers)) 0 else 1
    val values: Array<Any?> = if (receiverCount == 0) arguments.toTypedArray()
        else arrayOf(receiver, *arguments.toTypedArray())
    val types: Array<Class<*>> = if (receiverCount == 0) executable.parameterTypes
        else arrayOf(executable.declaringClass, *executable.parameterTypes)
    val receiver: Any? get() = if (receiverCount == 0) null else values[0]
    val resultType: Class<*> get() = (executable as? Method)?.returnType ?: Void.TYPE

    fun proceed(chain: Chain): Any? {
        val arguments = values.copyOfRange(receiverCount, values.size)
        return if (receiverCount == 0 || receiver === chain.thisObject) chain.proceed(arguments)
            else chain.proceedWith(requireNotNull(receiver), arguments)
    }
}

class ReturnValue(initialValue: Any? = null, initialThrowable: Throwable? = null) {
    var replace = false
        private set
    var throwable: Throwable? = initialThrowable
        set(value) {
            field = value
            if (value != null) replace = true
        }
    var result: Any? = initialValue
        set(value) {
            field = value
            throwable = null
            replace = true
        }
}
