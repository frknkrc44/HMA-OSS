# Permission metadata visibility probe

This probe calls the API-36 permission Binder service under a selected caller UID without launching the associated app. It checks an ungrouped permission enumeration, direct permission lookup, and preservation of platform permission/group metadata. It does not inspect account data or make network requests.

Compile `PermissionProbe.java` against the Android 36 SDK with Java 17 bytecode, then convert the class with Android's `d8` tool. Push the generated `classes.dex` to `/data/local/tmp/hma-permission-probe.dex`. Set its owner to root and mode to `0444` before running it: Android rejects writable DEX files for app UIDs.

Run the resulting DEX with this argument order:

```sh
# The final argument is the expected visibility under this caller's existing HMA scope.
CLASSPATH=/data/local/tmp/hma-permission-probe.dex app_process /system/bin PermissionProbe CALLER_PACKAGE TARGET_PERMISSION EXPECTED_VISIBLE
```

For caller-scoped verification, run the command through root's `su CALLER_UID -c '...'`, using the installed package's real UID. Run an unscoped caller control separately. The target should remain visible to the control; platform metadata should survive both cases. The probe exits with an assertion failure if enumeration/direct lookup differs from the expected visibility or platform metadata disappears.

Baseline on the current device before installing this fork: the KernelSU manager's dynamic-receiver permission was already absent for the scoped app UID and present for the unscoped shell UID. Both retained the platform permission and permission group. This does **not** reproduce a current permission-enumeration leak or establish the cause of an app's remaining detection popup. The fork adds explicit owner-based filtering to all four Binder metadata methods; its device registration and post-install behavior still need verification.

Framework contract: [Android 16 PermissionManagerService](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/services/core/java/com/android/server/pm/permission/PermissionManagerService.java). The list methods return `ParceledListSlice`; single lookups return their metadata object or null. No raw-list compatibility path is implemented.
