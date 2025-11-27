package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

@RequiresApi(Build.VERSION_CODES.R)
class PmsHookTarget30(service: HMAService) : PmsHookTargetBase(service) {

    companion object {
        private const val TAG = "PmsHookTarget30"
    }

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
        logI(TAG, "Load hook")

        findMethodOrNull("com.android.server.pm.PackageManagerService", findSuper = true) {
            name == "getPackageSetting"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingUid = Binder.getCallingUid()
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    logD(TAG, "@getPackageSetting - PkgMgr: insecure query from $caller to $targetApp")
                    param.result = null
                    service.filterCount++
                    return@hookBefore
                }
            }
        }?.let {
            hooks += it
        }

        hooks += findMethod("com.android.server.pm.AppsFilter") {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[0] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val targetApp = Utils4Xposed.getPackageNameFromPackageSettings(param.args[2])
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = true
                        service.filterCount++
                        val last = lastFilteredApp.getAndSet(caller)
                        if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                        logD(TAG, "@shouldFilterApplication caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }

       super.load()
    }
}
