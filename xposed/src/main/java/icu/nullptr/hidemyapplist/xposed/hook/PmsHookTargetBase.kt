package icu.nullptr.hidemyapplist.xposed.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
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
import icu.nullptr.hidemyapplist.xposed.logD
import java.util.concurrent.atomic.AtomicReference

abstract class PmsHookTargetBase(protected val service: HMAService) : IFrameworkHook {
    private val TAG by lazy { this::class.java.simpleName }

    protected val hooks = mutableListOf<XC_MethodHook.Unhook>()
    protected var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    protected val psSigningInfo by lazy {
        try {
            Utils.getPackageInfoCompat(
                service.pms,
                VENDING_PACKAGE_NAME,
                PackageManager.GET_SIGNING_CERTIFICATES.toLong(),
                0
            ).signingInfo
        } catch (_: Throwable) {
            null
        }
    }

    /*
    protected val psSigningDetails by lazy {
        // TODO: Implement vending signing details
        null
    }

    // TODO: Spoof PM install source
    internal val fakeSystemPackageInstallSource: Any? by lazy {
        try {
            findField("com.android.server.pm.InstallSource") {
                name == "EMPTY_ORPHANED"
            }.get(null)!!
        } catch (_: Throwable) {
            null
        }
    }

    abstract val fakeUserPackageInstallSource: Any?
    */

    abstract val fakeSystemPackageInstallSourceInfo: Any?
    abstract val fakeUserPackageInstallSourceInfo: Any?

    override fun load() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hooks += findMethod("com.android.server.pm.ComputerEngine") {
                name == "getPackageStates"
            }.hookAfter { param ->
                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookAfter

                val callingApps = Utils4Xposed.getCallingApps(service)
                for (caller in callingApps) {
                    if (service.isHookEnabled(caller)) {
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
                            logD(TAG, "@getPackageStates: removed ${markedToRemove.size} entries from $caller; ${result.size}, ${copyResult.size}")
                            param.result = copyResult
                            service.filterCount++

                            return@hookAfter
                        }
                    }
                }
            }
        }

        if (service.pmn != null) {
            findMethodOrNull(service.pmn::class.java, findSuper = true) {
                name == "getInstallerForPackage"
            }?.hookBefore { param ->
                val query = param.args[0] as String?
                val callingHandle = Binder.getCallingUserHandle()

                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingHandle.hashCode())
                } ?: return@hookBefore

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, callingHandle)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = "preload"
                        else -> continue
                    }
                }
            }?.let {
                logD(TAG, "PMN getInstallerForPackage is hooked!")
                hooks.add(it)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            /*
            findMethodOrNull(service.pms::class.java, findSuper = true) {
                name == "getInstallSource"
            }?.hookBefore { param ->
                val query = param.args[0] as String?
                val callingUid = param.args[1] as Int
                val user = UserHandle.getUserHandleForUid(param.args[2] as Int)

                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, user)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = fakeUserPackageInstallSource
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = fakeSystemPackageInstallSource
                        else -> continue
                    }
                }
            }?.let {
                hooks.add(it)
            }
            */

            findMethodOrNull(service.pms::class.java, findSuper = true) {
                name == "getInstallSourceInfo"
            }?.hookBefore { param ->
                val query = param.args[0] as String?

                val user = Binder.getCallingUserHandle()
                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore

                for (caller in callingApps) {
                    when (service.shouldHideInstallationSource(caller, query, user)) {
                        Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = fakeUserPackageInstallSourceInfo
                        Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = fakeSystemPackageInstallSourceInfo
                        else -> continue
                    }

                    service.filterCount++
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

            val user = Binder.getCallingUserHandle()
            val callingUid = Binder.getCallingUid()
            if (callingUid == Constants.UID_SYSTEM) return@hookBefore
            val callingApps = Utils.binderLocalScope {
                service.pms.getPackagesForUid(callingUid)
            } ?: return@hookBefore
            for (caller in callingApps) {
                when (service.shouldHideInstallationSource(caller, query, user)) {
                    Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                    Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = null
                    else -> continue
                }

                service.filterCount++
                break
            }
        }
    }

    final override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
