package org.frknkrc44.hma_oss.zygote.hook;

import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;

public class ABaseFrameworkHook {
    public final String TAG;

    ABaseFrameworkHook(String tag) {
        this.TAG = tag;
    }

    public void load() {
        logI(TAG, null, () -> "Load hook");
    }
}
