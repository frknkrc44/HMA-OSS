package android.os;

import androidx.annotation.RequiresApi;

public class ServiceManager {
    /**
     * Returns a reference to a service with the given name.
     *
     * @param name the name of the service to get
     * @return a reference to the service, or {@code null} if the service doesn't exist
     */
    public static IBinder getService(String name) {
        throw new RuntimeException("STUB");
    }

    /**
     * Returns the specified service from the service manager.
     * <br><br>
     * If the service is not running, ServiceManager will attempt to start it, and this function
     * will wait for it to be ready.
     *
     * @return {@code null} only if there are permission problems or fatal errors.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public static IBinder waitForService(String name) {
        throw new RuntimeException("STUB");
    }
}
