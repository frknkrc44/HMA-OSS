package icu.nullptr.hidemyapplist.xposed

object XposedConstants {
    const val COMPUTER_ENGINE_CLASS = "com.android.server.pm.ComputerEngine"
    const val PACKAGE_MANAGER_SERVICE_CLASS = "com.android.server.pm.PackageManagerService"
    const val PMS_COMPUTER_TRACKER_CLASS = $$"com.android.server.pm.PackageManagerService$ComputerTracker"
    const val APPS_FILTER_CLASS = "com.android.server.pm.AppsFilter"
    const val APPS_FILTER_IMPL_CLASS = "com.android.server.pm.AppsFilterImpl"
    const val STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService"
    const val ACCESSIBILITY_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService"
    const val CONTENT_PROVIDER_TRANSPORT_CLASS = $$"android.content.ContentProvider$Transport"
    const val IMM_SERVICE_CLASS = "com.android.server.inputmethod.InputMethodManagerService"
    const val PLATFORM_COMPAT_CLASS = "com.android.server.compat.PlatformCompat"
    const val ACTIVITY_STARTER_CLASS = "com.android.server.wm.ActivityStarter"
    const val ACTIVITY_TASK_SUPERVISOR_CLASS = "com.android.server.wm.ActivityTaskSupervisor"
    const val ACTIVITY_STACK_SUPERVISOR_CLASS = "com.android.server.wm.ActivityStackSupervisor"
}
