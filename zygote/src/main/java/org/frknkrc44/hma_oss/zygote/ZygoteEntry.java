package org.frknkrc44.hma_oss.zygote;

import static org.frknkrc44.hma_oss.zygote.util.Logcat.logELegacy;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logILegacy;

import android.os.Build;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.service.SystemServerHook;

import java.util.Locale;

@SuppressWarnings("all")
@DoNotObfuscateType
@DoNotShrinkType
public class ZygoteEntry {
    public static final String TAG = "ZygoteEntry";

    @DoNotObfuscate
    @DoNotShrink
    public static void premain() throws Throwable {
        var sdkLevel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                ? Build.VERSION.SDK_INT_FULL
                : Build.VERSION.SDK_INT;

        logILegacy(TAG, String.format(Locale.US, "Injecting into %s - SDK %d - %s",
                ZygoteLoader.getPackageName(), sdkLevel, BuildConfig.APP_VERSION_NAME));
    }

    @DoNotObfuscate
    @DoNotShrink
    public static void main() throws Throwable {
        try {
            SystemServerHook.init();
            logILegacy(TAG, "Done");
        } catch (Throwable th) {
            logELegacy(TAG, "An exception occurred while SystemServerHook init", th);
        }
    }
}
