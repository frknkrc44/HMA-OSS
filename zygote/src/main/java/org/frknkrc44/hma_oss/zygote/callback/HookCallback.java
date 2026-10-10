package org.frknkrc44.hma_oss.zygote.callback;

import com.v7878.unsafe.invoke.EmulatedStackFrame;

import org.frknkrc44.hma_oss.zygote.service.ReturnValue;

@FunctionalInterface
public interface HookCallback {
    void accept(String methodName, EmulatedStackFrame frame, ReturnValue returnValue) throws Throwable;
}
