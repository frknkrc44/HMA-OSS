package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService;

import android.app.Activity;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.system.Os;

import androidx.annotation.Nullable;

import org.frknkrc44.hma_oss.zygote.ZygoteEntry;

@SuppressWarnings("all")
public class ActivityManagerUtils {
    private ActivityManagerUtils() {}

    private static IActivityManager sActivityManager = null;

    public static void forceStopPackage(String packageName, int userId) throws RemoteException {
        getActivityManager().forceStopPackage(packageName, userId);
    }

    public static int startActivityNoThrow(Intent intent, int userId) {
        try {
            return startActivity(intent, userId);
        } catch (Throwable e) {
            logE(ZygoteEntry.TAG, e, () -> "Cannot start activity");
        }

        return Activity.RESULT_CANCELED;
    }

    public static int startActivity(Intent intent, int userId) throws RemoteException {
        return getActivityManager().startActivityAsUser(
                null,
                Os.getuid() == 2000 ? "com.android.shell" : null,
                intent,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                userId
        );
    }

    public static void registerUidObserver(IUidObserver observer, int which, int cutpoint, String callingPackage) throws RemoteException {
        getActivityManager().registerUidObserver(observer, which, cutpoint, callingPackage);
    }

    @Nullable
    public static IContentProvider getContentProviderExternal(@Nullable String name, int userId, @Nullable IBinder token, @Nullable String tag) throws RemoteException {
        final var external = getActivityManager().getContentProviderExternal(name, userId, token, tag);
        return external != null
                ? external.provider
                : null;
    }

    private static IActivityManager getActivityManager() {
        if (sActivityManager == null) {
            sActivityManager = IActivityManager.Stub.asInterface(waitForService(Context.ACTIVITY_SERVICE));
        }

        return sActivityManager;
    }
}
