package org.frknkrc44.hma_oss.xposed.service

import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import org.frknkrc44.hma_oss.xposed.util.Logcat.logE
import java.lang.reflect.Executable

/**
 * Explicit external ABI implementation. Do not replace this with a SAM lambda:
 * library R8 + app R8 can leave a synthetic Hooker class without intercept().
 */
internal class ProtectiveHooker(
    private val method: Executable,
    private val methodName: String,
    private val after: Boolean,
    private val callback: (String, HookFrame, ReturnValue) -> Unit,
) : Hooker {
    override fun intercept(chain: Chain): Any? =
        HookDispatch.intercept(chain, after, { error ->
            logE(TAG, error) { "Callback failed: $method" }
        }) { frame, value -> callback(methodName, frame, value) }

    private companion object {
        const val TAG = "HMA-LibXposed"
    }
}
