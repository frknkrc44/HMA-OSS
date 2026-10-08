package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.ContextUtils.getPackageManager;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod;
import static icu.nullptr.hidemyapplist.common.Utils.conflictedModules;

import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.List;

public class PackageManagerUtils {
    private PackageManagerUtils() {}

    public static boolean isConflictingModuleInstalled(IPackageManager pms) throws RemoteException {
        for (String app : conflictedModules) {
            if (pms.isPackageAvailable(app, 0)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    @Nullable
    public static Intent getLaunchIntentForPackageAsUser(String packageName, int userId) {
        final var intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);

        var resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);

            resolveInfos = queryIntentActivitiesAsUser(intentToResolve, userId);
        }

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        } else {
            final var intent = new Intent(intentToResolve);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            final var activityInfo = resolveInfos.get(0).activityInfo;
            intent.setClassName(activityInfo.packageName, activityInfo.name);

            return intent;
        }
    }

    // I am lazy to call IPackageManager
    @SuppressWarnings("all")
    @Nullable
    private static List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int userId) {
        try {
            return (List<ResolveInfo>) callMethod(
                    getPackageManager(),
                    "queryIntentActivitiesAsUser",
                    intent, 0, userId
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean findApp(IPackageManager pms, String packageName) throws RemoteException {
        for (int id : UserManagerUtils.getUserIds()) {
            if (pms.isPackageAvailable(packageName, id)) {
                return true;
            }
        }

        return false;
    }
}
