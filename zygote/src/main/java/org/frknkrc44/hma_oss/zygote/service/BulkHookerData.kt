package org.frknkrc44.hma_oss.zygote.service

import android.util.Pair
import com.v7878.unsafe.invoke.EmulatedStackFrame
import com.v7878.vmtools.HookTransformer
import org.frknkrc44.hma_oss.zygote.util.ZLUtils
import java.lang.invoke.MethodHandle
import java.lang.reflect.Executable

class ReturnValue(initialValue: Any? = null) {
    var replace: Boolean = false
        private set

    var result: Any? = initialValue
        set(newValue) {
            field = newValue
            replace = true
        }

    var throwable: Throwable? = null
}

data class HookParam(
    val clazz: String,
    val original: MethodHandle,
    val frame: EmulatedStackFrame,
    val methodName: String,
    val returnValue: ReturnValue,
) {
    var result: Any?
        get() = returnValue.result
        set(newValue) { returnValue.result = newValue }

    /**
     * @return Class of the return type
     */
    val returnType: Class<*> get() = frame.type().returnType()

    /**
     * Returns the first argument
     */
    val thisObject by lazy { ZLUtils.getArgument(frame, 0) }

    fun getArgument(index: Int) = ZLUtils.getArgument(frame, index)

    fun setArgument(index: Int, value: Any) = ZLUtils.setArgument(frame, index, value)

    /**
     * - `args[0] == thisObject`
     * - `args[1:] == function args`
     */
    val args by lazy { ZLUtils.dumpArgs(frame) }

    var throwable: Throwable?
        get() = returnValue.throwable
        set(newValue) { returnValue.throwable = newValue }
}

data class HookElement(
    val impl: HookTransformer,
    val methodName: String,
    val hookOnce: Boolean,
    var method: Executable? = null,
    var memoryAddresses: Pair<Long, Long>? = null,
    var hookFinished: Boolean = false,
    val paramCount: Int = -1,
    var applyCount: Int = 0,
)
