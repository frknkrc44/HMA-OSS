package org.frknkrc44.hma_oss.xposed.service

import io.github.libxposed.api.XposedInterface.Chain
import org.frknkrc44.hma_oss.xposed.util.ZLUtils.getArgument
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Executable

class HookDispatchTest {
    class Target {
        fun instance(value: String?): String? = value
        companion object {
            @JvmStatic fun staticCall(value: Int): Int = value
        }
    }

    private class Call(
        private val member: Executable = Target::class.java.getDeclaredMethod("instance", String::class.java),
        private val receiver: Any? = Target(),
        private val arguments: List<Any?> = listOf("original"),
        private val action: (Array<out Any?>) -> Any? = { it.lastOrNull() },
    ) : Chain {
        var calls = 0
        var received: Array<out Any?> = emptyArray()
        var receivedReceiver: Any? = null
        override fun getExecutable() = member
        override fun getThisObject() = receiver
        override fun getArgs() = arguments
        override fun getArg(index: Int) = arguments[index]
        override fun proceed() = proceed(arguments.toTypedArray())
        override fun proceed(args: Array<out Any?>): Any? = invoke(receiver, args)
        override fun proceedWith(thisObject: Any) = invoke(thisObject, arguments.toTypedArray())
        override fun proceedWith(thisObject: Any, args: Array<out Any?>): Any? = invoke(thisObject, args)
        private fun invoke(thisObject: Any?, args: Array<out Any?>): Any? {
            calls++
            received = args
            receivedReceiver = thisObject
            return action(args)
        }
    }

    private fun dispatch(call: Call, after: Boolean = false, callback: (HookFrame, ReturnValue) -> Unit) =
        HookDispatch.intercept(call, after, {}, callback)

    @Test fun instanceReceiverIsSeparateFromForwardedArguments() {
        val call = Call()
        val result = dispatch(call) { frame, _ ->
            assertSame(call.thisObject, frame.values[0])
            frame.values[1] = "changed"
        }
        assertEquals("changed", result)
        assertArrayEquals(arrayOf("changed"), call.received)
        assertEquals(1, call.calls)
    }

    @Test fun nullableReferenceArgumentPreservesUpstreamJvmBehavior() {
        val method = Target::class.java.getDeclaredMethod("instance", String::class.java)
        val frame = HookFrame(method, Target(), listOf(null))
        assertNull(frame.getArgument(1))
    }

    @Test fun staticArgumentsHaveNoReceiverSlot() {
        val call = Call(Target::class.java.getDeclaredMethod("staticCall", Int::class.javaPrimitiveType),
            null, listOf(3))
        assertEquals(7, dispatch(call) { frame, _ ->
            assertNull(frame.receiver)
            assertEquals(Int::class.javaPrimitiveType, frame.types[0])
            frame.values[0] = 7
        })
    }

    @Test fun explicitNullSkipsOriginal() {
        val call = Call()
        assertNull(dispatch(call) { _, value -> value.result = null })
        assertEquals(0, call.calls)
    }

    @Test fun afterReceivesAndReplacesOriginalResult() {
        val call = Call()
        assertEquals("replaced", dispatch(call, true) { _, value ->
            assertEquals("original", value.result)
            value.result = "replaced"
        })
        assertEquals(1, call.calls)
    }

    @Test fun beforeFailureDiscardsPartialChanges() {
        val call = Call()
        assertEquals("original", dispatch(call) { frame, value ->
            frame.values[1] = "partial"
            value.result = "partial"
            error("callback failed")
        })
        assertEquals(1, call.calls)
    }

    @Test fun afterFailureDoesNotInvokeOriginalAgain() {
        val call = Call()
        assertEquals("original", dispatch(call, true) { _, value ->
            value.result = "partial"
            error("callback failed")
        })
        assertEquals(1, call.calls)
    }

    @Test fun originalExceptionIsPreservedAndSkipsAfterCallback() {
        val error = IllegalStateException("original failed")
        val call = Call(action = { throw error })
        var callbackCalls = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            dispatch(call, true) { _, value ->
                callbackCalls++
                value.result = "recovered"
            }
        }
        assertSame(error, thrown)
        assertEquals(1, call.calls)
        assertEquals(0, callbackCalls)
    }

    @Test fun originalExceptionStackHidesHookImplementations() {
        val error = IllegalStateException("original").apply {
            stackTrace = arrayOf(
                StackTraceElement("android.content.Target", "call", "Target.java", 10),
                StackTraceElement("org.matrix.vector.impl.hooks.VectorChain", "proceed", "VectorChain.kt", 64),
                StackTraceElement("io.github.libxposed.api.XposedInterface", "proceed", "XposedInterface.java", 1),
                StackTraceElement("org.frknkrc44.hma_oss.xposed.service.HookDispatch", "intercept", "HookDispatch.kt", 15),
                StackTraceElement("com.example.Caller", "run", "Caller.kt", 20),
            )
        }
        val call = Call(action = { throw error })
        assertSame(error, assertThrows(IllegalStateException::class.java) {
            dispatch(call, true) { _, _ -> fail("after callback must not run") }
        })
        assertArrayEquals(
            arrayOf("android.content.Target", "com.example.Caller"),
            error.stackTrace.map { it.className }.toTypedArray(),
        )
    }

    @Test fun constructorKeepsReceiverAndVoidReturnType() {
        val call = Call(Target::class.java.getDeclaredConstructor(), Target(), emptyList(), { null })
        assertNull(dispatch(call) { frame, _ ->
            assertEquals(Void.TYPE, frame.resultType)
            assertSame(call.thisObject, frame.receiver)
            assertEquals(1, frame.values.size)
        })
        assertEquals(0, call.received.size)
        assertEquals(1, call.calls)
    }
}
