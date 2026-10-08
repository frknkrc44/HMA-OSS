package org.frknkrc44.hma_oss.zygote.util;

import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.IUserManager;
import android.os.RemoteException;

import java.util.List;

public class UserManagerUtils {
    private UserManagerUtils() {}

    private static final IUserManager userManager = (IUserManager) waitForService(Context.USER_SERVICE);

    public static List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException {
        try {
            return userManager.getUsers(excludeDying);
        } catch (Throwable ignored) {
            return userManager.getUsers(excludePartial, excludeDying, excludePreCreated);
        }
    }

    public static int[] getUserIds() {
        try {
            final var users = getUsers(false, false, false);
            final var idArr = new int[users.size()];

            for (int i = 0; i < idArr.length; i++) {
                idArr[i] = users.get(i).id;
            }

            return idArr;
        } catch (Throwable ignored) {
            return new int[] { 0 };
        }
    }
}
