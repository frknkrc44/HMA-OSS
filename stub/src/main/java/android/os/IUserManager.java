package android.os;

import androidx.annotation.NonNull;

public interface IUserManager extends IInterface {

    public @NonNull int[] getProfileIds(int userId, boolean enabledOnly)
            throws RemoteException;
}
