package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.zygote.util.CollectionUtils.firstOrNullWithType
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_IMPL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PERMISSION_MANAGER_SERVICE_CLASS

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
open class PmsHookTarget33 : PmsHookTargetBase() {
    override val TAG = "PmsHookTarget33"

    protected open val getPackagesForUidMethod by lazy {
        findMethod(
            "com.android.server.pm.Computer",
            "getPackagesForUid",
            isDeclared = false,
            systemClassLoader = true,
            Int::class.java,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        hooker.apply {
            // Samsung related fix
            if (OSUtils.isSamsung()) {
                hookAfter(
                    COMPUTER_ENGINE_CLASS,
                    "generatePackageInfo",
                ) { methodName, frame, returnValue ->
                    applyPackageHiding(
                        methodName,
                        returnValue,
                        { Binder.getCallingUid() },
                        { getPackageNameFromPackageSettings(frame.getArgument(1)) },
                        ::getCallingApps,
                        null,
                    )
                }
            } else {
                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "addPackageHoldingPermissions",
                ) { methodName, frame, returnValue ->
                    applyPackageHiding(
                        methodName,
                        returnValue,
                        { Binder.getCallingUid() },
                        { getPackageNameFromPackageSettings(frame.getArgument(2)) },
                        ::getCallingApps,
                        null,
                    )
                }
            }

            hookAfter(
                COMPUTER_ENGINE_CLASS,
                "getPackageInfoInternal",
            ) { methodName, frame, returnValue ->
                val args = frame.args

                applyPackageHiding(
                    methodName,
                    returnValue,
                    { args.firstOrNullWithType() },
                    { args.firstOrNullWithType() },
                    ::getCallingApps,
                    null,
                )
            }

            hookAfter(
                COMPUTER_ENGINE_CLASS,
                "getApplicationInfoInternal",
            ) { methodName, frame, returnValue ->
                val args = frame.args

                applyPackageHiding(
                    methodName,
                    returnValue,
                    { args.firstOrNullWithType() },
                    { args.firstOrNullWithType() },
                    ::getCallingApps,
                    null,
                )
            }

            hookBefore(
                APPS_FILTER_IMPL_CLASS,
                "shouldFilterApplication",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { frame.getArgument(2) as? Int },
                    { getPackageNameFromPackageSettings(frame.getArgument(4)) },
                    { _, it ->
                        Utils.binderLocalScope {
                            getPackagesForUidMethod.invoke(frame.getArgument(1), it) as? Array<String>
                        }
                    },
                    true,
                )
            }

            hookAfter(
                PERMISSION_MANAGER_SERVICE_CLASS,
                "checkUidPermission"
            ) { methodName, frame, returnValue ->
                if (returnValue.result == PackageManager.PERMISSION_DENIED) return@hookAfter

                val uid = frame.getArgument(1) as? Int ?: return@hookAfter

                applyPackageHiding(
                    methodName,
                    returnValue,
                    { Binder.getCallingUid() },
                    {
                        Utils.binderLocalScope {
                            getCallingApps(pms, uid).firstOrNull()
                        }
                    },
                    ::getCallingApps,
                    PackageManager.PERMISSION_DENIED
                )
            }
        }
    }
}
