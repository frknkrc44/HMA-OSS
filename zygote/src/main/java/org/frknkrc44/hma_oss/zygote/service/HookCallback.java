package org.frknkrc44.hma_oss.zygote.service;

import com.v7878.unsafe.invoke.EmulatedStackFrame;

@FunctionalInterface
public interface HookCallback {
    void accept(String methodName, EmulatedStackFrame frame, ReturnValue returnValue);
}
