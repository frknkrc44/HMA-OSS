package android.os;

import androidx.annotation.NonNull;

public interface IUserManager extends IInterface {

    public @NonNull int[] getProfileIds(int userId, boolean enabledOnly)
            throws RemoteException;

    abstract class Stub extends Binder implements IUserManager {

        public static IUserManager asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
