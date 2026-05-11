package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.UserHandle
import android.util.ArrayMap
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.service.HMAServiceCache
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.callMethod
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import java.util.concurrent.atomic.AtomicReference

abstract class PmsHookTargetBase(protected val service: HMAService) : IFrameworkHook {

    private val androidPkgClazzNames = arrayOf("AndroidPackage", "PackageImpl")

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
        BulkHooker.instance.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hookAfter(
                    COMPUTER_ENGINE_CLASS,
                    "getPackageStates",
                ) { param ->
                    val callingUid = Binder.getCallingUid()
                    if (callingUid == Constants.UID_SYSTEM) return@hookAfter

                    val callingApps = getCallingApps(service, callingUid)
                    val caller = callingApps.firstOrNull { service.isHookEnabled(it) }
                    if (caller != null) {
                        logD(TAG) { "@getPackageStates: incoming query from $caller" }

                        val result = param.result as ArrayMap<*, *>
                        val markedToRemove = mutableListOf<Any>()

                        for (pair in result.entries) {
                            val packageSettings = pair.value
                            val packageName = getPackageNameFromPackageSettings(packageSettings)
                            if (service.shouldHide(caller, packageName)) {
                                markedToRemove.add(pair.key)
                            }
                        }

                        if (markedToRemove.isNotEmpty()) {
                            val copyResult = ArrayMap(result)
                            copyResult.removeAll(markedToRemove)
                            logD(TAG) { "@getPackageStates: removed ${markedToRemove.size} entries from $caller" }
                            param.result = copyResult
                            service.increasePMFilterCount(caller)
                        }
                    }
                }

                // Samsung related fix
                if (OSUtils.isSamsung()) {
                    hookBefore(
                        COMPUTER_ENGINE_CLASS,
                        "generatePackageInfo",
                    ) { param ->
                        applyPackageHiding(
                            param.methodName,
                            { Binder.getCallingUid() },
                            { getPackageNameFromPackageSettings(param.getArgument(1)) },
                            { getCallingApps(service, it) },
                            { param.result = null },
                        )
                    }
                }

                // Samsung devices can fail to get this hook working,
                // but it is okay due to generatePackageInfo hook
                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "addPackageHoldingPermissions",
                ) { param ->
                    applyPackageHiding(
                        param.methodName,
                        { Binder.getCallingUid() },
                        { getPackageNameFromPackageSettings(param.getArgument(2)) },
                        { getCallingApps(service, it) },
                        { param.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "isCallerInstallerOfRecord",
                ) { param ->
                    val callingUid = param.args.last { it is Int } as Int

                    applyInstallerHiding(
                        param.methodName,
                        { callingUid },
                        fta@{
                            val pkg = param.args.lastOrNull {
                                it?.javaClass?.simpleName in androidPkgClazzNames
                            } ?: return@fta null
                            callMethod(pkg,
                                if (pkg.javaClass.simpleName == "PackageImpl") {
                                    "getManifestPackageName"
                                } else {
                                    "getPackageName"
                                }
                            ) as? String
                        }
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = callingUid == psPackageInfo?.applicationInfo?.uid
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = false
                        }
                    }
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getPackageInfoInternal",
                ) { param ->
                    applyPackageHiding(
                        param.methodName,
                        { param.args.firstOrNull { it is Int } as? Int },
                        { param.args.firstOrNull { it is String } as? String },
                        { getCallingApps(service, it) },
                        { param.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getApplicationInfoInternal",
                ) { param ->
                    applyPackageHiding(
                        param.methodName,
                        { param.args.firstOrNull { it is Int } as? Int },
                        { param.args.firstOrNull { it is String } as? String },
                        { getCallingApps(service, it) },
                        { param.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getInstallerPackageName",
                ) { param ->
                    applyInstallerHiding(
                        param.methodName,
                        { param.args.firstOrNull { it is Int } as? Int ?: Binder.getCallingUid() },
                        { param.args.firstOrNull { it is String } as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = null
                        }
                    }
                }
            } else {
                hookBefore(
                    service.pms.javaClass.name,
                    "getInstallerPackageName",
                ) { param ->
                    applyInstallerHiding(
                        param.methodName,
                        { Binder.getCallingUid() },
                        { param.getArgument(1) as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = null
                        }
                    }
                }
            }

            if (service.pmn != null) {
                hookBefore(
                    service.pmn.javaClass.name,
                    "getInstallerForPackage",
                ) { param ->
                    applyInstallerHiding(
                        param.methodName,
                        { Binder.getCallingUid() },
                        { param.getArgument(1) as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = "preload"
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hookBefore(
                    service.pms.javaClass.name,
                    "getInstallSourceInfo",
                ) { param ->
                    applyInstallerHiding(
                        param.methodName,
                        { Binder.getCallingUid() },
                        { param.getArgument(1) as? String }
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> param.result = fakeUserPackageInstallSourceInfo
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> param.result = fakeSystemPackageInstallSourceInfo
                        }
                    }
                }
            }
        }
    }

    fun applyPackageHiding(
        methodName: String,
        findCallingUid: () -> Int?,
        findTargetApp: () -> String?,
        findCallingApps: (Int) -> Array<String>?,
        applyReturnValue: () -> Unit,
    ) {
        val callingUid = findCallingUid()
        if (callingUid == null || callingUid == Constants.UID_SYSTEM) return
        val targetApp = findTargetApp() ?: return
        logV(TAG) { "@$methodName incoming query: $callingUid => $targetApp" }
        if (HMAServiceCache.instance.shouldHideFromUid(callingUid, targetApp) == true) {
            applyReturnValue()
            service.increasePMFilterCount(callingUid)
            logD(TAG) { "@$methodName caller cache: $callingUid, target: $targetApp" }
            return
        }
        val callingApps = findCallingApps(callingUid)
        val caller = callingApps?.firstOrNull { service.shouldHide(it, targetApp) }
        if (caller != null) {
            logD(TAG) { "@$methodName caller: $callingUid $caller, target: $targetApp" }
            applyReturnValue()
            val last = lastFilteredApp.getAndSet(caller)
            if (last != caller) logI(TAG) { "@${methodName}: query from $caller" }
            HMAServiceCache.instance.putShouldHideUidCache(callingUid, caller, targetApp)
            service.increasePMFilterCount(caller)
        }
    }

    fun applyInstallerHiding(
        methodName: String,
        findCallingUid: () -> Int?,
        findTargetApp: () -> String?,
        applyReturnValue: (Int) -> Unit,
    ) {
        val callingUid = findCallingUid() ?: return
        if (callingUid == Constants.UID_SYSTEM) return

        val callingApps = getCallingApps(service, callingUid)
        val callingHandle = UserHandle.getUserHandleForUid(callingUid)

        val query = findTargetApp() ?: return

        for (caller in callingApps) {
            val isHide = service.shouldHideInstallationSource(caller, query, callingHandle)
            if (isHide == Constants.FAKE_INSTALLATION_SOURCE_DISABLED) continue

            logD(TAG) { "@$methodName: Applied installer hiding for $caller - $callingUid => $isHide" }

            applyReturnValue(isHide)

            service.increaseInstallerFilterCount(caller)
            break
        }
    }
}
