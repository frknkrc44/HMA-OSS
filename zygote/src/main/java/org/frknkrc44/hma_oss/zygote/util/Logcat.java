package org.frknkrc44.hma_oss.zygote.util;

import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.Nullable;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.service.UserService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class Logcat {
    private Logcat() {}

    private static final boolean logdReady = "running".equals(SystemProperties.get("init.svc.logd"));
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);

    public static void logV(String tag, @Nullable Throwable cause, Supplier<String> message) {
        logWithLevel(Log.VERBOSE, tag, cause, message);
    }

    public static void logD(String tag, @Nullable Throwable cause, Supplier<String> message) {
        logWithLevel(Log.DEBUG, tag, cause, message);
    }

    public static void logI(String tag, @Nullable Throwable cause, Supplier<String> message) {
        logWithLevel(Log.INFO, tag, cause, message);
    }

    public static void logW(String tag, @Nullable Throwable cause, Supplier<String> message) {
        logWithLevel(Log.WARN, tag, cause, message);
    }

    public static void logE(String tag, @Nullable Throwable cause, Supplier<String> message) {
        logWithLevel(Log.ERROR, tag, cause, message);
    }

    public static void logWithLevel(int level, String tag, @Nullable Throwable cause, Supplier<String> message) {
        final var service = UserService.getService();
        if (service != null) {
            if (level != Log.ERROR && service.config.getErrorOnlyLog()) return;
            if (level <= Log.DEBUG && !service.config.getDetailLog()) return;
        }
        if (level == Log.VERBOSE && !BuildConfig.DEBUG) return;

        final var levelStr = switch (level) {
            case Log.VERBOSE -> "VERBS";
            case Log.DEBUG   -> "DEBUG";
            case Log.INFO    -> " INFO";
            case Log.WARN    -> " WARN";
            case Log.ERROR   -> "ERROR";
            default -> "?WTF";
        };

        final var date = dateFormat.format(new Date());
        final var builder = new StringBuilder()
                .append("[")
                .append(levelStr)
                .append("] <")
                .append(date)
                .append("> (")
                .append(tag)
                .append(") ")
                .append(message.get());
        if (builder.charAt(builder.length()-1) != '\n') builder.append('\n');
        if (cause != null) {
            builder.append(Log.getStackTraceString(cause));
            if (builder.charAt(builder.length()-1) != '\n') builder.append('\n');
        }
        final var builtString = builder.toString();

        if (service != null) {
            executor.execute(() -> {
                try {
                    service.addLog(builtString);
                } catch (Throwable ignore) {}
            });
        }

        if (logdReady) {
            Log.i("HMA-OSS", builtString);
        }
    }
}
