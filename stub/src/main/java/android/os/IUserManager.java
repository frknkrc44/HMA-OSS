package android.os;

import android.content.pm.UserInfo;

import androidx.annotation.NonNull;

import java.util.List;

public interface IUserManager extends IInterface {

    public @NonNull int[] getProfileIds(int userId, boolean enabledOnly)
            throws RemoteException;

    public List<UserInfo> getUsers(boolean excludeDying)
            throws RemoteException;

    public List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated)
            throws RemoteException;

    abstract class Stub extends Binder implements IUserManager {

        public static IUserManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
