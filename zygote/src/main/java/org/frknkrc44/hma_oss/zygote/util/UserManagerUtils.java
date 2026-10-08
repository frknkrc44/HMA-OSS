package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.ZygoteEntry.TAG;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logV;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.IUserManager;
import android.os.RemoteException;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.List;

public class UserManagerUtils {
    private UserManagerUtils() {}

    private static final IUserManager userManager =
            IUserManager.Stub.asInterface(waitForService(Context.USER_SERVICE));

    public static List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException {
        try {
            return userManager.getUsers(excludeDying);
        } catch (Throwable e) {
            // noinspection ConstantValue
            if (!(e instanceof NoSuchMethodException)) {
                logV(TAG, e, () -> "An unknown error occurred while executing getUsers");

                throw new RemoteException(e.getMessage());
            }
        }

        try {
            return userManager.getUsers(excludePartial, excludeDying, excludePreCreated);
        } catch (Throwable e) {
            if (!(e instanceof NoSuchMethodException)) {
                logV(TAG, e, () -> "An unknown error occurred while executing getUsers");

                throw new RemoteException(e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    public static int[] getUserIds() throws RemoteException {
        final var users = getUsers(false, false, false);
        if (users.isEmpty()) return new int[] { 0 };

        final var ids = new ArraySet<Integer>();

        for (int i = 0; i < users.size(); i++) {
            final var id = users.get(i).id;
            ids.add(id);

            for (int item : userManager.getProfileIds(id, false)) {
                ids.add(item);
            }
        }

        return ids.stream().mapToInt(Integer::intValue).toArray();
    }
}
