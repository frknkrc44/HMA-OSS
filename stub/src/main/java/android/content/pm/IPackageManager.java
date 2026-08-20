package android.content.pm;

import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IPackageManager extends IInterface {

    boolean isPackageAvailable(String packageName, int userId)
            throws RemoteException;

    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId)
            throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    PackageInfo getPackageInfo(String packageName, long flags, int userId)
            throws RemoteException;

    @RequiresApi(24)
    int getPackageUid(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    int getPackageUid(String packageName, long flags, int userId)
            throws RemoteException;

    String[] getPackagesForUid(int uid)
            throws RemoteException;
}
