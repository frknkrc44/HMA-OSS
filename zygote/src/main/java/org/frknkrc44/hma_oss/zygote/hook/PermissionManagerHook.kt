package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import org.frknkrc44.hma_oss.zygote.util.CollectionUtils.firstWithType
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackagesForUidLocal
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.dumpArgs
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PERMISSION_MANAGER_SERVICE_CLASS

class PermissionManagerHook : PmsHookTargetBase() {
    override val TAG = "PermissionManagerHook"

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
}
