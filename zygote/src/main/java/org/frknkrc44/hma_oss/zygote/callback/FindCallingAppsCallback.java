package org.frknkrc44.hma_oss.zygote.callback;

import android.os.RemoteException;

@FunctionalInterface
public interface FindCallingAppsCallback {
    String[] accept(int callingUid) throws RemoteException;
}
