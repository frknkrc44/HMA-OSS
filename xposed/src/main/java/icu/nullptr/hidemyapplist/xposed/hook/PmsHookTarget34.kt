package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.PackageInstaller
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

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PmsHookTarget34(service: HMAService) : PmsHookTargetBase(service) {

    companion object {
        private const val TAG = "PmsHookTarget34"
    }

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 6
        }.newInstance(
            null,
            null,
            null,
            null,
            null,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 6
        }.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            PackageInstaller.PACKAGE_SOURCE_STORE,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, "Load hook")
        hooks += findMethod("com.android.server.pm.AppsFilterImpl", findSuper = true) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val snapshot = param.args[0]
                val callingUid = param.args[1] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    getPackagesForUidMethod.invoke(snapshot, callingUid) as Array<String>?
                } ?: return@hookBefore
                val targetApp = Utils4Xposed.getPackageNameFromPackageSettings(param.args[3]) // PackageSettings <- PackageStateInternal
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
        // AOSP exploit - https://github.com/aosp-mirror/platform_frameworks_base/commit/5bc482bd99ea18fe0b4064d486b29d5ae2d65139
        // Only 14 QPR2+ has this method
        findMethodOrNull("com.android.server.pm.PackageManagerService", findSuper = true) {
            name == "getArchivedPackageInternal"
        }?.hookBefore { param ->
            runCatching {
                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val targetApp = param.args[0].toString()
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = null
                        service.filterCount++
                        val last = lastFilteredApp.getAndSet(caller)
                        if (last != caller) logI(TAG, "@getArchivedPackageInternal: query from $caller")
                        logD(TAG, "@getArchivedPackageInternal caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }?.let {
            hooks.add(it)
        }

        super.load()
    }
}
