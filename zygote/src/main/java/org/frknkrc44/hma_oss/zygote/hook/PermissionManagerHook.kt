package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
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
                    {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            frame.getArgument(3) as? Int
                        } else {
                            Binder.getCallingUid()
                        }
                    },
                    {
                        val packageNameIndex = when (Build.VERSION.SDK_INT) {
                            Build.VERSION_CODES.Q, Build.VERSION_CODES.R -> 2
                            else -> 1
                        }

                        frame.getArgument(packageNameIndex) as? String
                    },
                    ::getPackagesForUidLocal,
                    PackageManager.PERMISSION_DENIED
                )
            }

            hookBefore(
                arrayOf(COMPUTER_ENGINE_CLASS, PERMISSION_MANAGER_SERVICE_CLASS),
                arrayOf("checkUidPermission"),
            ) { methodName, frame, returnValue ->
                val args = frame.dumpArgs()

                applyPackageHiding(
                    methodName,
                    returnValue,
                    {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            args[4] as? Int
                        } else {
                            Binder.getCallingUid()
                        }
                    },
                    {
                        val uid = args.firstWithType<Int>()
                        getPackagesForUidLocal(pms, uid).firstOrNull()
                    },
                    ::getPackagesForUidLocal,
                    PackageManager.PERMISSION_DENIED
                )
            }
        }
    }
}
