package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.findField;
import static icu.nullptr.hidemyapplist.common.Utils.containsMultiple;
import static icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.Magic;
import org.frknkrc44.hma_oss.zygote.callback.BinderLocalScopeCallback;
import org.frknkrc44.hma_oss.zygote.callback.CallerCheckerCallback;

import java.util.ArrayList;
import java.util.List;

import icu.nullptr.hidemyapplist.common.Constants;
import icu.nullptr.hidemyapplist.common.JsonConfig;
import icu.nullptr.hidemyapplist.common.PropertyUtils;

public class ServiceUtils {
    private ServiceUtils() {}

    private static final String TAG = "ServiceUtils";

    public static <T> T binderLocalScope(BinderLocalScopeCallback<T> block) throws RemoteException {
        final var identity = Binder.clearCallingIdentity();
        final var result = block.accept();
        Binder.restoreCallingIdentity(identity);
        return result;
    }

    @Nullable
    public static <T> T binderLocalScopeNoThrow(BinderLocalScopeCallback<T> block) {
        try {
            return binderLocalScope(block);
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while binderLocalScopeNoThrow");
        }

        return null;
    }

    public static void binderLocalScopeNoReturn(Runnable block) {
        final var identity = Binder.clearCallingIdentity();
        try {
            block.run();
        } catch (Throwable e) {
            logE(TAG, e, () -> "An error occurred while binderLocalScopeNoReturn");
        }
        Binder.restoreCallingIdentity(identity);
    }

    public static IBinder waitForService(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return ServiceManager.waitForService(name);
        }

        int count = 0;
        IBinder service;
        while ((service = ServiceManager.getService(name)) == null && ++count < 200) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return service;
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    public static String getPackageNameFromPackageSettings(@Nullable Object packageSettings) {
        if (packageSettings == null) return null;

        try {
            return (String) callMethod(packageSettings, "getPackageName");
        } catch (Throwable ignored) {
            try {
                final var field = findField(
                        packageSettings.getClass(),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "mName" : "name"
                );
                field.setAccessible(true);
                return (String) field.get(packageSettings);
            } catch (Throwable ignored2) {}

            return null;
        }
    }

    public static String[] getCallingApps(IPackageManager pms) throws RemoteException {
        return getCallingApps(pms, Binder.getCallingUid());
    }

    public static String[] getCallingApps(IPackageManager pms, int callingUid) throws RemoteException {
        if (callingUid == Constants.UID_SYSTEM) return new String[0];

        final var result = binderLocalScope(() -> pms.getPackagesForUid(callingUid));
        return result != null
                ? result
                : new String[0];
    }

    public static int findAndVerifyAppSignature(IPackageManager pms) {
        try {
            final var profiles = UserManagerUtils.getUserIds();

            for (int uid : profiles) {
                logV(TAG, null, () -> "@findAndVerifyAppSignature: checking for " + uid);

                PackageInfo packageInfo = null;
                try {
                    packageInfo = getPackageInfoCompat(
                            pms,
                            BuildConfig.APP_PACKAGE_NAME,
                            PackageManager.GET_SIGNING_CERTIFICATES,
                            uid
                    );
                } catch (Throwable ignored) {}

                if (packageInfo == null) continue;

                if (packageInfo.applicationInfo != null && verifyAppSignature(packageInfo)) {
                    final var appUid = packageInfo.applicationInfo.uid;

                    logI(TAG, null, () -> "The manager app signature is verified successfully, uid: " + appUid);

                    return appUid;
                } else {
                    throw new Exception("The manager app is modified, skipping");
                }
            }
        } catch (Throwable e) {
            logE(TAG, e, () -> "Fatal: Cannot get package details\nCompile this app from source with your changes");

            return -1;
        }

        logE(TAG, null, () -> "The manager app is not found, skipping");

        return -1;
    }

    private static boolean verifyAppSignature(@NonNull PackageInfo packageInfo) {
        final var signingInfo = packageInfo.signingInfo;
        if (signingInfo == null) return false;

        final var history = signingInfo.getSigningCertificateHistory();
        if (history == null || history.length < 1) return false;

        final var bytes = history[history.length - 1].toByteArray();
        if (Magic.magicNumbers.length != bytes.length) return false;

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != Magic.magicNumbers[i]) return false;
        }

        return true;
    }

    public static void clearStackTraces(Throwable throwable) {
        while (throwable != null) {
            final var newTrace = new ArrayList<>(List.of(throwable.getStackTrace()));
            newTrace.removeIf(e ->
                    containsMultiple(
                            e.getClassName(),
                            "BulkHooker", "com.v7878", "MethodHandle", BuildConfig.APP_PACKAGE_NAME
                    ) || containsMultiple(
                            e.getFileName(),
                            "r8-map-id-", "dex-id-", "ReplaceMePls"
                    )
            );

            if (newTrace.size() != throwable.getStackTrace().length) {
                throwable.setStackTrace(newTrace.toArray(new StackTraceElement[0]));
            }

            throwable = throwable.getCause();
        }
    }

    public static boolean isAppDataIsolationEnabled(JsonConfig config) {
        return PropertyUtils.isAppDataIsolationEnabled() || config.getAltAppDataIsolation();
    }

    @Nullable
    public static String getScopedCaller(String[] callingApps, CallerCheckerCallback checker) {
        for (String caller : callingApps) {
            if (checker.accept(caller)) {
                return caller;
            }
        }

        return null;
    }
}
