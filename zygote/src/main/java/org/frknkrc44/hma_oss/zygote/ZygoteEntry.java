package org.frknkrc44.hma_oss.zygote;

import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.callStaticMethod;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_NATIVE_SERVICE;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.RUNTIME_INIT_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.SYSTEM_SERVER_CLASS;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_INIT_CLASS;

import android.content.pm.IPackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.vmtools.HookTransformer;
import com.v7878.vmtools.Hooks;
import com.v7878.zygisk.ZygoteLoader;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.service.UserService;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("all")
@DoNotObfuscateType
@DoNotShrinkType
public class ZygoteEntry {
    public static final String TAG = "ZygoteEntry";

    @Nullable
    public static ClassLoader classLoader;

    private static final Executor executor = Executors.newSingleThreadExecutor();

    @DoNotObfuscate
    @DoNotShrink
    public static void premain() throws Throwable {
        logI(TAG, null, () -> String.format("Injecting into %s - %s", ZygoteLoader.getPackageName(), BuildConfig.APP_VERSION_NAME));
    }

    @DoNotObfuscate
    @DoNotShrink
    public static void main() throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            logI(TAG, null, () -> "Trying to invoke 12+ mode");

            try {
                final var loader = callStaticMethod(
                        Class.forName(ZYGOTE_INIT_CLASS),
                        "getOrCreateSystemServerClassLoader"
                );

                if (loader != null && loader instanceof ClassLoader classLoader) {
                    onSystemServer(classLoader);

                    return;
                } else {
                    throw new RuntimeException("Class loader is null, aborting");
                }
            } catch (Throwable e) {
                logE(TAG, e, () -> "An exception occurred while trying 12+ mode");
                // falls back to 11- mode
            }
        }

        logI(TAG, null, () -> "Trying to invoke 11- mode");

        final var method = getDeclaredMethod(
                Class.forName(RUNTIME_INIT_CLASS), "findStaticMain",
                String.class, String[].class, ClassLoader.class
        );

        Hooks.hook(method, Hooks.EntryPointType.CURRENT, new HookTransformer() {
            @Override
            public void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable {
                try {
                    final var accessor = frame.accessor();
                    if (SYSTEM_SERVER_CLASS.equals(accessor.getReference(0))) {
                        onSystemServer((ClassLoader) accessor.getReference(2));
                    }
                } catch (Throwable e) {
                    logE(TAG, e, () -> "An exception occurred while findStaticMain");
                }
            }
        }, Hooks.EntryPointType.DIRECT);
    }

    private static void onSystemServer(@NonNull ClassLoader loader) {
        logV(TAG, null, () -> "Class loader found: " + loader);

        classLoader = loader;

        executor.execute(() -> {
            final var pms = waitForService(PACKAGE_MANAGER_SERVICE);
            final var pmn = waitForService(PACKAGE_MANAGER_NATIVE_SERVICE);
            logD(TAG, null, () -> "Got pms: " + pms + ", " + pmn);

            try {
                UserService.register((IPackageManager) pms, pmn);
                logI(TAG, null, () -> "System service started");
            } catch (Throwable e) {
                logE(TAG, e, () -> "System service crashed");
            }
        });
    }
}
