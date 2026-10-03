package android.content.pm;

import android.os.Build;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import java.util.List;

public interface IPackageManager extends IInterface {

    boolean isPackageAvailable(String packageName, int userId)
            throws RemoteException;

    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
            throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId)
            throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    PackageInfo getPackageInfo(String packageName, long flags, int userId)
            throws RemoteException;

    int getPackageUid(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    int getPackageUid(String packageName, long flags, int userId)
            throws RemoteException;

    String[] getPackagesForUid(int uid)
            throws RemoteException;

    List<String> getAllPackages()
            throws RemoteException;
}
