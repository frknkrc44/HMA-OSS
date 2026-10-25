import android.content.pm.PermissionInfo;
import android.os.IBinder;
import android.os.Process;
import java.lang.reflect.Method;
import java.util.List;

/** Exercises the real permission Binder service under a chosen test caller UID, without launching that app. */
public final class PermissionProbe {
    public static void main(String[] args) throws Exception {
        String callerPackage = args[0];
        String targetPermission = args[1];
        boolean expectedVisible = Boolean.parseBoolean(args[2]);

        // Reflection reaches the same hidden Binder interface the framework uses on the API-36 test device.
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager.getMethod("getService", String.class)
                .invoke(null, "permissionmgr");
        Class<?> stub = Class.forName("android.permission.IPermissionManager$Stub");
        Object service = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        Class<?> api = Class.forName("android.permission.IPermissionManager");

        Object slice = api.getMethod("queryPermissionsByGroup", String.class, int.class)
                .invoke(service, null, 0);
        Method getList = Class.forName("android.content.pm.ParceledListSlice").getMethod("getList");
        getList.setAccessible(true);
        List<?> permissions = (List<?>) getList.invoke(slice);
        boolean enumerated = permissions.stream()
                .map(item -> (PermissionInfo) item)
                .anyMatch(item -> targetPermission.equals(item.name));

        // Compare direct lookup with enumeration; filtering just one surface leaves the package inferable.
        Method lookup = api.getMethod("getPermissionInfo", String.class, String.class, int.class);
        boolean lookedUp = lookup.invoke(service, targetPermission, callerPackage, 0) != null;
        boolean platformRetained = lookup.invoke(service, "android.permission.INTERNET", callerPackage, 0) != null;
        Object groups = api.getMethod("getAllPermissionGroups", int.class).invoke(service, 0);
        boolean platformGroupRetained = api.getMethod("getPermissionGroupInfo", String.class, int.class)
                .invoke(service, "android.permission-group.LOCATION", 0) != null;

        System.out.println("uid=" + Process.myUid() + " enumerated=" + enumerated
                + " directLookup=" + lookedUp + " platformPermission=" + platformRetained
                + " platformGroup=" + platformGroupRetained
                + " groupResult=" + groups.getClass().getSimpleName());
        // Unscoped controls must retain the target; scoped callers must lose it while platform metadata survives.
        if (enumerated != expectedVisible || lookedUp != expectedVisible
                || !platformRetained || !platformGroupRetained) {
            throw new AssertionError("Permission visibility differs from the expected caller scope");
        }
    }
}
