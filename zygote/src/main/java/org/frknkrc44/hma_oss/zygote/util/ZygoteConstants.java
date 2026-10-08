package org.frknkrc44.hma_oss.zygote.util;

public class ZygoteConstants {
    private ZygoteConstants() {}

    public static final String SYSTEM_SERVER_CLASS = "com.android.server.SystemServer";
    public static final String RUNTIME_INIT_CLASS = "com.android.internal.os.RuntimeInit";
    public static final String ZYGOTE_INIT_CLASS = "com.android.internal.os.ZygoteInit";
    public static final String COMPUTER_ENGINE_CLASS = "com.android.server.pm.ComputerEngine";
    public static final String PACKAGE_MANAGER_SERVICE_CLASS = "com.android.server.pm.PackageManagerService";
    public static final String PMS_COMPUTER_TRACKER_CLASS = "com.android.server.pm.PackageManagerService$ComputerTracker";
    public static final String PMS_COMPUTER_ENGINE_CLASS = "com.android.server.pm.PackageManagerService$ComputerEngine";
    public static final String APPS_FILTER_CLASS = "com.android.server.pm.AppsFilter";
    public static final String APPS_FILTER_IMPL_CLASS = "com.android.server.pm.AppsFilterImpl";
    public static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService";
    public static final String STORAGE_MANAGER_SERVICE_LIFECYCLE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    public static final String ACCESSIBILITY_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService";
    public static final String CONTENT_PROVIDER_TRANSPORT_CLASS = "android.content.ContentProvider$Transport";
    public static final String IMM_SERVICE_CLASS = "com.android.server.inputmethod.InputMethodManagerService";
    public static final String IMM_IMPL_CLASS = "com.android.server.inputmethod.IInputMethodManagerImpl";
    public static final String ACTIVITY_STARTER_CLASS = "com.android.server.wm.ActivityStarter";
    public static final String ACTIVITY_TASK_SUPERVISOR_CLASS = "com.android.server.wm.ActivityTaskSupervisor";
    public static final String ACTIVITY_STACK_SUPERVISOR_CLASS = "com.android.server.wm.ActivityStackSupervisor";
    public static final String ZYGOTE_PROCESS_CLASS = "android.os.ZygoteProcess";
    public static final String NATIVE_ZYGOTE_PROCESS_CLASS = "android.os.NativeZygoteProcess";
    public static final String PROCESS_LIST_CLASS = "com.android.server.am.ProcessList";
    public static final String PROCESS_RECORD_INTERNAL_CLASS = "com.android.server.am.psc.ProcessRecordInternal";
    public static final String BROADCAST_CONTROLLER_CLASS = "com.android.server.am.BroadcastController";
    public static final String ACTIVITY_MANAGER_SERVICE_CLASS = "com.android.server.am.ActivityManagerService";
    public static final String BROADCAST_HELPER_CLASS = "com.android.server.pm.BroadcastHelper";
    public static final String PACKAGE_MONITOR_CLASS = "com.android.internal.content.PackageMonitor";
    public static final String SERVICE_RECORD_CLASS = "com.android.server.am.ServiceRecord";
    public static final String BROADCAST_QUEUE_CLASS = "com.android.server.am.BroadcastQueue";
    public static final String BROADCAST_QUEUE_IMPL_CLASS = "com.android.server.am.BroadcastQueueImpl";
    public static final String BROADCAST_PROCESS_QUEUE_CLASS = "com.android.server.am.BroadcastProcessQueue";

    public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    public static final String PACKAGE_MANAGER_SERVICE = "package";
    public static final String PACKAGE_MANAGER_NATIVE_SERVICE = "package_native";
    public static final String WEBVIEW_UPDATE_SERVICE = "webviewupdate";

    public static final String WEBVIEW_PROVIDER_KEY = "webview_provider";

    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String USB_FUNCTION_ADB = "adb";

    public static final String GBOARD_PACKAGE_NAME = "com.google.android.inputmethod.latin";
    public static final String GBOARD_CLASS_NAME = "com.android.inputmethod.latin.LatinIME";
}
