package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageItemInfo
import android.content.pm.ParceledListSlice
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.os.Binder
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PERMISSION_MANAGER_SERVICE_CLASS

/**
 * Hides permissions that belong to hidden packages from the global permission registry.
 *
 * Permission metadata exposes its defining package even when package visibility is filtered.
 * Apply the existing caller-scoped hiding policy to that metadata without changing permission grants
 * or results seen by system services and unscoped apps.
 */
class PermissionHook : IFrameworkHook {
    override val TAG = "PermissionHook"

    override fun load() {
        logI(TAG) { "Load hook" }

        hooker.apply {
            // Primary leak: listing ungrouped permissions yields one entry per installed package.
            hookAfter(PERMISSION_MANAGER_SERVICE_CLASS, "queryPermissionsByGroup", 2) { methodName, _, returnValue ->
                filterListResult<PermissionInfo>(methodName, returnValue)
            }
            // Groups also carry a defining packageName; hide groups owned by a hidden package.
            hookAfter(PERMISSION_MANAGER_SERVICE_CLASS, "getAllPermissionGroups", 1) { methodName, _, returnValue ->
                filterListResult<PermissionGroupInfo>(methodName, returnValue)
            }
            // Direct single lookups would otherwise confirm a hidden package by name.
            hookAfter(PERMISSION_MANAGER_SERVICE_CLASS, "getPermissionInfo", 3) { methodName, _, returnValue ->
                filterSingleResult(methodName, returnValue)
            }
            hookAfter(PERMISSION_MANAGER_SERVICE_CLASS, "getPermissionGroupInfo", 2) { methodName, _, returnValue ->
                filterSingleResult(methodName, returnValue)
            }
        }
    }

    // Caller context, or null for system / non-scoped callers so they pay only one getPackagesForUid call.
    private fun resolveCaller(): Triple<Int, Array<String>, Int>? {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Constants.UID_SYSTEM) return null

        val callingApps = ServiceUtils.getCallingApps(pms, callingUid)
        if (callingApps.isEmpty()) return null
        if (callingApps.none { config.scope.containsKey(it) }) return null

        return Triple(callingUid, callingApps, getUserFromCallingUid(callingUid))
    }

    // Reuses the same per-uid hide cache and decision logic as the package hooks for consistency.
    private fun shouldHidePackage(uid: Int, apps: Array<String>, userId: Int, pkg: String?): Boolean {
        if (pkg == null) return false

        if (dataHolder.shouldHideFromUid(uid, pkg) == true) {
            service.increasePMFilterCount(uid)
            return true
        }

        val caller = apps.firstOrNull { service.shouldHide(it, pkg, userId) } ?: return false
        dataHolder.putShouldHideUidCache(uid, caller, pkg)
        service.increasePMFilterCount(caller)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : PackageItemInfo> filterListResult(methodName: String, returnValue: ReturnValue) {
        if (returnValue.throwable != null) return

        // The binder-facing methods return slices; retain their exact PermissionInfo/PermissionGroupInfo types.
        val result = returnValue.result as? ParceledListSlice<T> ?: return
        val list = result.list
        if (list.isEmpty()) return

        val (uid, apps, userId) = resolveCaller() ?: return

        val kept = list.filterNot { item ->
            shouldHidePackage(uid, apps, userId, item.packageName)
        }
        val removed = list.size - kept.size
        if (removed == 0) return

        returnValue.result = ParceledListSlice(kept)
        logD(TAG) { "@$methodName: hid $removed permission entries from uid $uid" }
    }

    private fun filterSingleResult(methodName: String, returnValue: ReturnValue) {
        if (returnValue.throwable != null) return

        val item = returnValue.result as? PackageItemInfo ?: return
        val (uid, apps, userId) = resolveCaller() ?: return

        if (shouldHidePackage(uid, apps, userId, item.packageName)) {
            // Null makes PackageManager throw NameNotFoundException, i.e. "no such permission".
            returnValue.result = null
            logD(TAG) { "@$methodName: hid ${item.packageName} from uid $uid" }
        }
    }
}
