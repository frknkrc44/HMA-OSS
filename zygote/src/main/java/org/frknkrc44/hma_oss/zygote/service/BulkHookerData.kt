package org.frknkrc44.hma_oss.zygote.service

import android.util.Pair
import com.v7878.vmtools.HookTransformer
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
