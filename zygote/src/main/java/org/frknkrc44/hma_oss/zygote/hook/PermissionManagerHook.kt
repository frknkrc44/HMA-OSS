package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.Binder
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.CollectionUtils.firstWithType
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackagesForUidLocal
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PERMISSION_MANAGER_SERVICE_CLASS
import java.util.concurrent.atomic.AtomicReference

class PermissionManagerHook : IFrameworkHook {
    override val TAG = "PermissionManagerHook"

    @PublishedApi
    internal val lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    override fun load() {
        hooker.apply {
            hookBefore(
                PERMISSION_MANAGER_SERVICE_CLASS,
                "checkPermission",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { Binder.getCallingUid() },
                    { frame.getArgument(1) as? String },
                    ::getPackagesForUidLocal,
                    PackageManager.PERMISSION_DENIED
                )
            }

            hookBefore(
                arrayOf(COMPUTER_ENGINE_CLASS, PERMISSION_MANAGER_SERVICE_CLASS),
                arrayOf("checkUidPermission"),
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { Binder.getCallingUid() },
                    {
                        val uid = frame.dumpArgs().firstWithType<Int>()
                        getPackagesForUidLocal(pms, uid).firstOrNull()
                    },
                    ::getPackagesForUidLocal,
                    PackageManager.PERMISSION_DENIED
                )
            }
        }
    }

    inline fun applyPackageHiding(
        methodName: String,
        returnValue: ReturnValue,
        findCallingUid: () -> Int?,
        findTargetApp: () -> String?,
        findCallingApps: (IPackageManager, Int) -> Array<String>?,
        valueForHiding: Any?,
    ) {
        val callingUid = findCallingUid()
        if (callingUid == null || callingUid == Constants.UID_SYSTEM) return

        val targetApp = findTargetApp() ?: return
        logV(TAG) { "@$methodName incoming query: $callingUid => $targetApp" }
        if (dataHolder.shouldHideFromUid(callingUid, targetApp) == true) {
            returnValue.result = valueForHiding
            service.increasePMFilterCount(callingUid)
            logD(TAG) { "@$methodName caller cache: $callingUid, target: $targetApp" }
            return
        }
        val callingApps = findCallingApps(pms, callingUid)
        val caller = callingApps?.firstOrNull { service.shouldHide(it, targetApp) }
        if (caller != null) {
            logD(TAG) { "@$methodName caller: $callingUid $caller, target: $targetApp" }
            returnValue.result = valueForHiding
            val last = lastFilteredApp.getAndSet(caller)
            if (last != caller) logI(TAG) { "@$methodName: query from $caller" }
            dataHolder.putShouldHideUidCache(callingUid, caller, targetApp)
            service.increasePMFilterCount(caller)
        }
    }
}
