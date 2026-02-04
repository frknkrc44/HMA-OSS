package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.UserHandle
import android.util.ArrayMap
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.XposedConstants.COMPUTER_ENGINE_CLASS
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logV
import java.util.concurrent.atomic.AtomicReference

abstract class PmsHookTargetBase(protected val service: HMAService) : IFrameworkHook {

    @Suppress("PropertyName")
    abstract val TAG: String

    protected val hooks = mutableListOf<XC_MethodHook.Unhook>()
    protected var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    protected val psPackageInfo by lazy {
        try {
            Utils.getPackageInfoCompat(
                service.pms,
                VENDING_PACKAGE_NAME,
                PackageManager.GET_SIGNING_CERTIFICATES.toLong(),
                0
            )
        } catch (_: Throwable) {
            null
        }
    }

    abstract val fakeSystemPackageInstallSourceInfo: Any?
    abstract val fakeUserPackageInstallSourceInfo: Any?

    override fun load() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hooks += findMethod(COMPUTER_ENGINE_CLASS) {
                name == "getPackageStates"
            }.hookAfter { param ->
                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookAfter

                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                val caller = callingApps.firstOrNull { service.isHookEnabled(it) }
                if (caller != null) {
                    logD(TAG, "@getPackageStates: incoming query from $caller")

                    val result = param.result as ArrayMap<*, *>
                    val markedToRemove = mutableListOf<Any>()

                    for (pair in result.entries) {
                        val value = pair.value
                        val packageName = XposedHelpers.callMethod(value, "getPackageName") as String
                        if (service.shouldHide(caller, packageName)) {
                            markedToRemove.add(pair.key)
                        }
                    }

                    if (markedToRemove.isNotEmpty()) {
                        val copyResult = ArrayMap(result)
                        copyResult.removeAll(markedToRemove)
                        logD(TAG, "@getPackageStates: removed ${markedToRemove.size} entries from $caller")
                        param.result = copyResult
                        service.increasePMFilterCount(caller)
                    }
                }
            }

            findMethodOrNull(COMPUTER_ENGINE_CLASS) {
                name == "addPackageHoldingPermissions"
            }?.hookBefore { param ->
                val callingUid = Binder.getCallingUid()
                val packageState = param.args[1] ?: return@hookBefore
                val targetApp = XposedHelpers.callMethod(packageState, "getPackageName") as String? ?: return@hookBefore
                if (service.shouldHideFromUid(callingUid, targetApp) == true) {
                    param.result = null
                    service.increasePMFilterCount(callingUid)
                    logD(TAG, "@addPackageHoldingPermissions caller cache: $callingUid, target: $targetApp")
                    return@hookBefore
                }
                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                val caller = callingApps.firstOrNull { service.shouldHide(it, targetApp) }
                if (caller != null) {
                    logD(TAG, "@addPackageHoldingPermissions caller: $callingUid $caller, target: $targetApp")
                    param.result = null
                    service.putShouldHideUidCache(callingUid, caller, targetApp)
                    service.increasePMFilterCount(caller)
                }
            }?.let {
                logD(TAG, "CE addPackageHoldingPermissions is hooked!")
                hooks += it
            }

            hooks += findMethod(COMPUTER_ENGINE_CLASS) {
                name == "isCallerInstallerOfRecord"
            }.hookBefore { param ->
                val pkg = param.args.first()
                val query = XposedHelpers.callMethod(pkg, "getPackageName") as String

                val callingUid = param.args.last() as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                val callingHandle = UserHandle.getUserHandleForUid(callingUid)

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, callingHandle)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = callingUid == psPackageInfo?.applicationInfo?.uid
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = false
                        else -> continue
                    }

                    service.increaseInstallerFilterCount(caller)
                    break
                }
            }

            hooks += findMethod(COMPUTER_ENGINE_CLASS) {
                name == "getPackageInfoInternal"
            }.hookBefore { param ->
                val targetApp = param.args.firstOrNull { it is String } as? String ?: return@hookBefore
                val callingUid = Binder.getCallingUid()
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

            hooks += findMethod(COMPUTER_ENGINE_CLASS) {
                name == "getApplicationInfoInternal"
            }.hookBefore { param ->
                val targetApp = param.args.firstOrNull { it is String } as? String ?: return@hookBefore
                val callingUid = Binder.getCallingUid()
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
        }

        if (service.pmn != null) {
            findMethodOrNull(service.pmn::class.java, findSuper = true) {
                name == "getInstallerForPackage"
            }?.hookBefore { param ->
                val query = param.args[0] as String?

                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                val callingHandle = UserHandle.getUserHandleForUid(callingUid)

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, callingHandle)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = "preload"
                        else -> continue
                    }

                    service.increaseInstallerFilterCount(caller)
                    break
                }
            }?.let {
                logD(TAG, "PMN getInstallerForPackage is hooked!")
                hooks.add(it)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            findMethodOrNull(service.pms::class.java, findSuper = true) {
                name == "getInstallSourceInfo"
            }?.hookBefore { param ->
                val query = param.args[0] as String?

                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
                val callingHandle = UserHandle.getUserHandleForUid(callingUid)

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, callingHandle)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = fakeUserPackageInstallSourceInfo
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = fakeSystemPackageInstallSourceInfo
                        else -> continue
                    }

                    service.increaseInstallerFilterCount(caller)
                    break
                }
            }?.let {
                hooks.add(it)
            }
        }

        hooks += findMethod(service.pms::class.java, findSuper = true) {
            name == "getInstallerPackageName"
        }.hookBefore { param ->
            val query = param.args[0] as String?

            val callingUid = Binder.getCallingUid()
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore

            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            val callingHandle = UserHandle.getUserHandleForUid(callingUid)

            for (caller in callingApps) {
                when (service.shouldHideInstallationSource(caller, query, callingHandle)) {
                    Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                    Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = null
                    else -> continue
                }

                service.increaseInstallerFilterCount(caller)
                break
            }
        }
    }

    final override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
