package org.frknkrc44.hma_oss.zygote.service;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.v7878.vmtools.HookTransformer;

import java.lang.reflect.Executable;

public class HookElement {
    public HookElement(HookTransformer impl, @NonNull String methodName, int argumentCount) {
        this.impl = impl;
        this.methodName = methodName;
        this.argumentCount = argumentCount;
    }

    public final HookTransformer impl;

    @NonNull
    public final String methodName;

    @Nullable
    public Executable executable;

    @Nullable
    public Pair<Long, Long> memoryAddresses;

    public boolean hookFinished = false;

    public final int argumentCount;
}
