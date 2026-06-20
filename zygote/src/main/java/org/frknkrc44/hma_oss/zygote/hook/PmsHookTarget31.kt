package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArg
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PMS_COMPUTER_TRACKER_CLASS

@RequiresApi(Build.VERSION_CODES.S)
class PmsHookTarget31(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget31"

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            4,
        )!!.newInstance(
            null,
            null,
            null,
            null,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            4,
        )!!.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
        )
    }

    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageSetting",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { Binder.getCallingUid() },
                    { frame.getArg(1) as String? },
                    { getCallingApps(service, it) },
                    { returnValue.result = null },
                )
            }

            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageSettingInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArg(2) as Int? },
                    { frame.getArg(1) as String? },
                    { getCallingApps(service, it) },
                    { returnValue.result = null },
                )
            }

            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getPackageInfoInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArg(4) as Int? },
                    { frame.getArg(1) as String? },
                    { getCallingApps(service, it) },
                    { returnValue.result = null },
                )
            }

            hookBefore(
                PMS_COMPUTER_TRACKER_CLASS,
                "getApplicationInfoInternal",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArg(3) as Int? },
                    { frame.getArg(1) as String? },
                    { getCallingApps(service, it) },
                    { returnValue.result = null },
                )
            }

            hookBefore(
                APPS_FILTER_CLASS,
                "shouldFilterApplication",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArg(1) as Int? },
                    { getPackageNameFromPackageSettings(frame.getArg(3)) },
                    { getCallingApps(service, it) },
                    { returnValue.result = true },
                )
            }
        }

        super.load()
    }
}
