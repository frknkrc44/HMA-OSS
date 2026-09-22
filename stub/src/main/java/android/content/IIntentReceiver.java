package android.content;

import android.os.Bundle;
import android.os.RemoteException;

public interface IIntentReceiver {
    void performReceive(Intent intent, int resultCode, String data,
                        Bundle extras, boolean ordered, boolean sticky, int sendingUser)
            throws RemoteException;
}
