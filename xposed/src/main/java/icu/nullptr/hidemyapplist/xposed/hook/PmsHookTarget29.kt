package icu.nullptr.hidemyapplist.xposed.hook

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Logcat.logI
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed.getCallingApps
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed.getPackageNameFromPackageSettings

class PmsHookTarget29(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget29"

    // not required until SDK 30
    override val fakeSystemPackageInstallSourceInfo = null
    override val fakeUserPackageInstallSourceInfo = null

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        hooks += findMethod(service.pms::class.java, findSuper = true) {
            name == "filterAppAccessLPr" && parameterCount == 5
        }.hookBefore { param ->
            applyPackageHiding(
                param.method.name,
                { param.args[1] as Int? },
                { getPackageNameFromPackageSettings(param.args[0]) },
                { getCallingApps(service, it) },
                { param.result = true },
            )
        }

        super.load()
    }
}
