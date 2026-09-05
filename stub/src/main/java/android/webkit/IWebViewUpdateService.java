package android.webkit;

import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public interface IWebViewUpdateService extends IInterface {

    @RequiresApi(30)
    public @Nullable PackageInfo getCurrentWebViewPackage()
            throws RemoteException;

    public @Nullable String getCurrentWebViewPackageName()
            throws RemoteException;

    abstract class Stub extends Binder implements IWebViewUpdateService {

        public static IWebViewUpdateService asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
