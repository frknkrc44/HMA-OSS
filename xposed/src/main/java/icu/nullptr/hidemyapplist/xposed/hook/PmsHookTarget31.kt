package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Logcat.logI
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed.getCallingApps
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed.getPackageNameFromPackageSettings
import icu.nullptr.hidemyapplist.xposed.XposedConstants.APPS_FILTER_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PMS_COMPUTER_TRACKER_CLASS

@RequiresApi(Build.VERSION_CODES.S)
class PmsHookTarget31(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget31"

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 4
        }.newInstance(
            null,
            null,
            null,
            null,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 4
        }.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
        )
    }

    override fun load() {
        logI(TAG) { "Load hook" }

        findMethodOrNull(PMS_COMPUTER_TRACKER_CLASS) {
            name == "getPackageSetting"
        }?.hookBefore { param ->
            applyPackageHiding(
                param.method.name,
                { Binder.getCallingUid() },
                { param.args[0] as String? },
                { getCallingApps(service, it) },
                { param.result = null },
            )
        }?.let {
            hooks += it
        }

        findMethodOrNull(PMS_COMPUTER_TRACKER_CLASS) {
            name == "getPackageSettingInternal"
        }?.hookBefore { param ->
            applyPackageHiding(
                param.method.name,
                { param.args[1] as Int? },
                { param.args[0] as String? },
                { getCallingApps(service, it) },
                { param.result = null },
            )
        }?.let {
            hooks += it
        }

        hooks += findMethod(APPS_FILTER_CLASS) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            applyPackageHiding(
                param.method.name,
                { param.args[0] as Int? },
                { getPackageNameFromPackageSettings(param.args[2]) },
                { getCallingApps(service, it) },
                { param.result = true },
            )
        }

       super.load()
    }
}
