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
import icu.nullptr.hidemyapplist.xposed.XposedConstants.APPS_FILTER_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PACKAGE_MANAGER_SERVICE_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PMS_COMPUTER_TRACKER_CLASS
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import icu.nullptr.hidemyapplist.xposed.logV

@RequiresApi(Build.VERSION_CODES.S)
class PmsHookTarget31(service: HMAService) : PmsHookTargetBase(service) {

    companion object {
        private const val TAG = "PmsHookTarget31"
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

        findMethodOrNull(PMS_COMPUTER_TRACKER_CLASS) {
            name == "getPackageSetting"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingUid = Binder.getCallingUid()
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore
            if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                param.result = null
                service.increasePMFilterCount(callingUid)
                logD(TAG, "@getPackageSetting - Computer cache: insecure query from $callingUid to $targetApp")
                return@hookBefore
            }
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
            if (caller != null) {
                logD(TAG, "@getPackageSetting - Computer: insecure query from $caller to $targetApp")
                param.result = null
                service.putShouldHideUidCache(callingUid, caller, targetApp)
                service.increasePMFilterCount(caller)
            }
        }?.let {
            hooks += it
        }

        findMethodOrNull(PMS_COMPUTER_TRACKER_CLASS) {
            name == "getPackageSettingInternal"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingUid = param.args[1] as Int
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore
            if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                param.result = null
                service.increasePMFilterCount(callingUid)
                logD(TAG, "@getPackageSettingInternal - Computer cache: insecure query from $callingUid to $targetApp")
                return@hookBefore
            }
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
            if (caller != null) {
                logD(TAG, "@getPackageSettingInternal - Computer: insecure query from $caller to $targetApp")
                param.result = null
                service.putShouldHideUidCache(callingUid, caller, targetApp)
                service.increasePMFilterCount(caller)
            }
        }?.let {
            hooks += it
        }

        hooks += findMethod(APPS_FILTER_CLASS) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[0] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val targetApp = Utils4Xposed.getPackageNameFromPackageSettings(param.args[2])
                if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                    param.result = true
                    service.increasePMFilterCount(callingUid)
                    logD(TAG, "@shouldFilterApplication caller cache: $callingUid, target: $targetApp")
                    return@hookBefore
                }
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
                if (caller != null) {
                    param.result = true
                    service.putShouldHideUidCache(callingUid, caller, targetApp!!)
                    service.increasePMFilterCount(caller)
                    val last = lastFilteredApp.getAndSet(caller)
                    if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                    logD(TAG, "@shouldFilterApplication caller: $callingUid $caller, target: $targetApp")
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }

        hooks += findMethod(PACKAGE_MANAGER_SERVICE_CLASS) {
            name == "getPackageInfoInternal"
        }.hookBefore { param ->
            val targetApp = param.args.first() as String? ?: return@hookBefore
            val callingUid = param.args[3] as Int
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore
            logV(TAG, "@${param.method.name} incoming query: $callingUid => $targetApp")
            if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                param.result = null
                service.increasePMFilterCount(callingUid)
                logD(TAG, "@${param.method.name} caller cache: $callingUid, target: $targetApp")
                return@hookBefore
            }
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
            if (caller != null) {
                logD(TAG, "@${param.method.name} caller: $callingUid $caller, target: $targetApp")
                param.result = null
                service.putShouldHideUidCache(callingUid, caller, targetApp)
                service.increasePMFilterCount(caller)
            }
        }

        hooks += findMethod(PMS_COMPUTER_TRACKER_CLASS) {
            name == "getApplicationInfoInternal"
        }.hookBefore { param ->
            val targetApp = param.args.first() as String? ?: return@hookBefore
            val callingUid = param.args[2] as Int
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore
            logV(TAG, "@${param.method.name} incoming query: $callingUid => $targetApp")
            if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                param.result = null
                service.increasePMFilterCount(callingUid)
                logD(TAG, "@${param.method.name} caller cache: $callingUid, target: $targetApp")
                return@hookBefore
            }
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
            if (caller != null) {
                logD(TAG, "@${param.method.name} caller: $callingUid $caller, target: $targetApp")
                param.result = null
                service.putShouldHideUidCache(callingUid, caller, targetApp)
                service.increasePMFilterCount(caller)
            }
        }

       super.load()
    }
}
