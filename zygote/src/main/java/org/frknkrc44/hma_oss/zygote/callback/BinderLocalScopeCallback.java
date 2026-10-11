package org.frknkrc44.hma_oss.zygote.callback;

import android.os.RemoteException;

@FunctionalInterface
public interface BinderLocalScopeCallback<T> {
    T accept() throws RemoteException;
}
