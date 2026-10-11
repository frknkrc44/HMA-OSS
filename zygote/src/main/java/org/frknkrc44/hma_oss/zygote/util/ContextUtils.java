package org.frknkrc44.hma_oss.zygote.util;

import android.app.ActivityThread;
import android.app.Application;
import android.content.ContentResolver;
import android.content.pm.PackageManager;

public class ContextUtils {
    private ContextUtils() {}

    public static Application getApplication() {
        return ActivityThread.currentActivityThread().getApplication();
    }

    public static PackageManager getPackageManager() {
        return getApplication().getPackageManager();
    }

    public static ContentResolver getContentResolver() {
        return getApplication().getContentResolver();
    }
}
