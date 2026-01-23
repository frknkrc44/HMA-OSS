package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.XposedConstants.COMPUTER_ENGINE_CLASS
import icu.nullptr.hidemyapplist.xposed.XposedConstants.PACKAGE_MANAGER_SERVICE_CLASS
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import icu.nullptr.hidemyapplist.xposed.logV

class PmsHookTarget28(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget28"

    // not required until SDK 30
    override val fakeSystemPackageInstallSourceInfo = null
    override val fakeUserPackageInstallSourceInfo = null

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, "Load hook")

        hooks += findMethod(service.pms::class.java, findSuper = true) {
            name == "filterAppAccessLPr" && parameterCount == 5
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[1] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val packageSettings = param.args[0] ?: return@hookBefore
                val targetApp = Utils4Xposed.getPackageNameFromPackageSettings(packageSettings)
                if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                    param.result = true
                    service.increasePMFilterCount(callingUid)
                    logD(TAG, "@filterAppAccessLPr caller cache: $callingUid, target: $targetApp")
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
                    if (last != caller) logI(TAG, "@filterAppAccessLPr query from $caller")
                    logD(TAG, "@filterAppAccessLPr caller: $callingUid $caller, target: $targetApp")
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

        hooks += findMethod(PACKAGE_MANAGER_SERVICE_CLASS) {
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
