package org.frknkrc44.hma_oss.zygote;

import static org.frknkrc44.hma_oss.zygote.util.Logcat.logELegacy;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logILegacy;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.service.SystemServerHook;

@SuppressWarnings("all")
@DoNotObfuscateType
@DoNotShrinkType
public class ZygoteEntry {
    public static final String TAG = "ZygoteEntry";

    @DoNotObfuscate
    @DoNotShrink
    public static void premain() throws Throwable {

    }

    @DoNotObfuscate
    @DoNotShrink
    public static void main() throws Throwable {
        logILegacy(TAG, String.format("Injected into %s - %s", ZygoteLoader.getPackageName(), BuildConfig.APP_VERSION_NAME), null);

        try {
            SystemServerHook.init();
        } catch (Throwable th) {
            logELegacy(TAG, "An exception occurred while SystemServerHook init", th);

            // do not print "Done" if there is an issue
            return;
        }

        logILegacy(TAG, "Done", null);
    }
}
