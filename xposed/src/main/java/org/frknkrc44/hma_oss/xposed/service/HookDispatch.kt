package org.frknkrc44.hma_oss.xposed.service

import io.github.libxposed.api.XposedInterface.Chain
import org.frknkrc44.hma_oss.xposed.util.ServiceUtils

internal object HookDispatch {
    fun intercept(
        chain: Chain,
        after: Boolean,
        onError: (Throwable) -> Unit,
        callback: (HookFrame, ReturnValue) -> Unit,
    ): Any? {
        val frame = HookFrame(chain.executable, chain.thisObject, chain.args)
        var originalResult: Any? = null
        if (after) {
            originalResult = proceedOriginal { frame.proceed(chain) }
        }
        val value = ReturnValue(originalResult)
        try {
            callback(frame, value)
        } catch (error: Throwable) {
            onError(error)
            // Discard partial callback changes; never execute the original twice.
            if (after) return originalResult
            return proceedOriginal { chain.proceed() }
        }
        value.throwable?.let(::rethrow)
        return if (after || value.replace) value.result
            else proceedOriginal { frame.proceed(chain) }
    }

    private inline fun proceedOriginal(block: () -> Any?): Any? = try {
        block()
    } catch (error: Throwable) {
        rethrow(error)
    }

    private fun rethrow(error: Throwable): Nothing {
        ServiceUtils.clearStackTraces(error)
        throw error
    }
}
