package org.frknkrc44.hma_oss.zygote.service;

import androidx.annotation.Nullable;

public class ReturnValue {

    @Nullable
    private Object result;

    private boolean replace = false;

    @Nullable
    public Throwable throwable;

    public ReturnValue(@Nullable Object initialValue) {
        result = initialValue;
    }

    public boolean isReplaced() {
        return replace;
    }

    @Nullable
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        replace = true;
        this.result = result;
    }
}
