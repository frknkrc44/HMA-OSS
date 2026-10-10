package org.frknkrc44.hma_oss.zygote.service;

import static org.frknkrc44.hma_oss.zygote.util.ActivityManagerUtils.getContentProviderExternal;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logD;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logE;
import static org.frknkrc44.hma_oss.zygote.util.Logcat.logI;
import static org.frknkrc44.hma_oss.zygote.util.ServiceUtils.waitForService;
import static org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField;
import static org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_MANAGER_CLASS;
import static icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid;

import android.content.AttributionSource;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

import org.frknkrc44.hma_oss.common.BuildConfig;
import org.frknkrc44.hma_oss.zygote.util.ActivityManagerUtils;
import org.frknkrc44.hma_oss.zygote.util.UidObserverAdapter;

import java.lang.reflect.InvocationTargetException;

import icu.nullptr.hidemyapplist.common.Constants;

public class UserService {
    private UserService() {}

    private static final String TAG = "HMA-UserService";

    public static HMAService service;

    private static final UidObserverAdapter uidObserver = new UidObserverAdapter() {
        @Override
        public void onUidActive(int uid) throws RemoteException {
            final var appUid = getManagerAppUid();

            if (appUid < 0 || uid != appUid) {
                return;
            }

            try {
                final var userId = getUserFromCallingUid(uid);

                logD(TAG, null,  () -> "Calculated user id: " + userId);

                final var provider = getContentProviderExternal(
                        Constants.PROVIDER_AUTHORITY, userId, null, null);
                assert provider != null : "Failed to get provider";

                final var extras = new Bundle();
                extras.putBinder("binder", service);

                Bundle reply;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    final var attr = new AttributionSource.Builder(1000)
                            .setPackageName("android")
                            .build();
                    reply = provider.call(attr, Constants.PROVIDER_AUTHORITY, "", null, extras);
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    reply = provider.call("android", null, Constants.PROVIDER_AUTHORITY, "", null, extras);
                } else {
                    reply = provider.call("android", Constants.PROVIDER_AUTHORITY, "", null, extras);
                }

                if (reply == null) {
                    logE(TAG, null, () -> "Failed to send binder to app");
                    return;
                }

                logI(TAG, null, () -> "Sent binder to app");
            } catch (Throwable e) {
                logE(TAG, e, () -> "onUidActive");
            }
        }
    };

    public static void register(IPackageManager pms, Object pmn) throws ClassNotFoundException, IllegalAccessException, RemoteException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        assert service == null : "You cannot register the service more than once";

        logI(TAG, null, () -> "Initialize HMAService - Version " + BuildConfig.APP_VERSION_NAME);

        waitForService(Context.ACTIVITY_SERVICE);
        ActivityManagerUtils.registerUidObserver(
                uidObserver,
                getActMgrField("UID_OBSERVER_ACTIVE"),
                getActMgrField("PROCESS_STATE_TOP"),
                null
        );

        logI(TAG, null, () -> "Registered observer");

        // no need to save it
        new HMAService(pms, pmn);
    }

    private static int getActMgrField(String name) throws ClassNotFoundException, IllegalAccessException {
        return getStaticIntField(ACTIVITY_MANAGER_CLASS, name);
    }

    private static int getManagerAppUid() {
        return service != null
                ? service.appUid
                : -1;
    }
}
