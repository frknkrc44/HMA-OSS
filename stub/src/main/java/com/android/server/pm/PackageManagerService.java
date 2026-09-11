package com.android.server.pm;

import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.server.pm.permission.PermissionManagerServiceInternal;

public interface PackageManagerService extends IInterface {

    public String getDefaultBrowserPackageName(int userId)
        throws RemoteException;

    @RequiresApi(30)
    public PermissionManagerServiceInternal getPermissionManagerServiceInternal()
            throws RemoteException;

    @RequiresApi(31)
    DefaultAppProvider getDefaultAppProvider()
            throws RemoteException;
}
