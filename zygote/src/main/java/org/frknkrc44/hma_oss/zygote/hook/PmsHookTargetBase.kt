package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.util.ArrayMap
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.lastWithType
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.service.HMAServiceCache
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import java.util.concurrent.atomic.AtomicReference

abstract class PmsHookTargetBase : IFrameworkHook {

    private val androidPkgClazzNames = arrayOf("AndroidPackage", "PackageImpl")

    protected var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    protected val psPackageInfo by lazy {
        val pms = service?.pms ?: return@lazy null
        try {
            pms.getPackageInfoCompat(
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
                ) { _, _, returnValue ->
                    val callingUid = Binder.getCallingUid()
                    if (callingUid == Constants.UID_SYSTEM) return@hookAfter

                    val callingUserId = getUserFromCallingUid(callingUid)

                    val callingApps = getCallingApps(callingUid)
                    val caller = callingApps.firstOrNull { service?.isHookEnabled(it) ?: false }
                    if (caller != null) {
                        logD(TAG) { "@getPackageStates: incoming query from $caller" }

                        val result = returnValue.result as ArrayMap<*, *>
                        val markedToRemove = mutableListOf<Any>()

                        for (pair in result.entries) {
                            val packageSettings = pair.value
                            val packageName = getPackageNameFromPackageSettings(packageSettings)
                            if (service?.shouldHide(caller, packageName, callingUserId) ?: false) {
                                markedToRemove.add(pair.key)
                            }
                        }

                        if (markedToRemove.isNotEmpty()) {
                            val copyResult = ArrayMap(result)
                            copyResult.removeAll(markedToRemove)
                            logD(TAG) { "@getPackageStates: removed ${markedToRemove.size} entries from $caller" }
                            returnValue.result = copyResult
                            service?.increasePMFilterCount(caller)
                        }
                    }
                }

                // Samsung related fix
                if (OSUtils.isSamsung()) {
                    hookBefore(
                        COMPUTER_ENGINE_CLASS,
                        "generatePackageInfo",
                    ) { methodName, frame, returnValue ->
                        applyPackageHiding(
                            methodName,
                            { Binder.getCallingUid() },
                            { getPackageNameFromPackageSettings(frame.getArgument(1)) },
                            ::getCallingApps,
                            { returnValue.result = null },
                        )
                    }
                }

                // Samsung devices can fail to get this hook working,
                // but it is okay due to generatePackageInfo hook
                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "addPackageHoldingPermissions",
                ) { methodName, frame, returnValue ->
                    applyPackageHiding(
                        methodName,
                        { Binder.getCallingUid() },
                        { getPackageNameFromPackageSettings(frame.getArgument(2)) },
                        ::getCallingApps,
                        { returnValue.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getPackageInfoInternal",
                ) { methodName, frame, returnValue ->
                    applyPackageHiding(
                        methodName,
                        { frame.args.firstOrNullWithType() },
                        { frame.args.firstOrNullWithType() },
                        ::getCallingApps,
                        { returnValue.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getApplicationInfoInternal",
                ) { methodName, frame, returnValue ->
                    applyPackageHiding(
                        methodName,
                        { frame.args.firstOrNullWithType() },
                        { frame.args.firstOrNullWithType() },
                        ::getCallingApps,
                        { returnValue.result = null },
                    )
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "isCallerInstallerOfRecord",
                ) { methodName, frame, returnValue ->
                    val callingUid = frame.args.lastWithType<Int>()

                    applyInstallerHiding(
                        methodName,
                        { callingUid },
                        fta@{
                            val pkg = frame.args.lastOrNull {
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
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = callingUid == psPackageInfo?.applicationInfo?.uid
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = false
                        }
                    }
                }

                hookBefore(
                    COMPUTER_ENGINE_CLASS,
                    "getInstallerPackageName",
                ) { methodName, frame, returnValue ->
                    applyInstallerHiding(
                        methodName,
                        { frame.args.firstOrNullWithType() ?: Binder.getCallingUid() },
                        { frame.args.firstOrNullWithType() },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = null
                        }
                    }
                }
            } else {
                hookBefore(
                    service!!.pms.javaClass.name,
                    "getInstallerPackageName",
                ) { methodName, frame, returnValue ->
                    applyInstallerHiding(
                        methodName,
                        { Binder.getCallingUid() },
                        { frame.getArgument(1) as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = null
                        }
                    }
                }
            }

            if (service?.pmn != null) {
                hookBefore(
                    service!!.pmn!!.javaClass.name,
                    "getInstallerForPackage",
                ) { methodName, frame, returnValue ->
                    applyInstallerHiding(
                        methodName,
                        { Binder.getCallingUid() },
                        { frame.getArgument(1) as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = "preload"
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hookBefore(
                    service!!.pms.javaClass.name,
                    "getInstallSourceInfo",
                ) { methodName, frame, returnValue ->
                    applyInstallerHiding(
                        methodName,
                        { Binder.getCallingUid() },
                        { frame.getArgument(1) as? String }
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = fakeUserPackageInstallSourceInfo
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = fakeSystemPackageInstallSourceInfo
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
            service?.increasePMFilterCount(callingUid)
            logD(TAG) { "@$methodName caller cache: $callingUid, target: $targetApp" }
            return
        }
        val callingUserId = getUserFromCallingUid(callingUid)
        val callingApps = findCallingApps(callingUid)
        val caller = callingApps?.firstOrNull { service?.shouldHide(it, targetApp, callingUserId) ?: false }
        if (caller != null) {
            logD(TAG) { "@$methodName caller: $callingUid $caller, target: $targetApp" }
            applyReturnValue()
            val last = lastFilteredApp.getAndSet(caller)
            if (last != caller) logI(TAG) { "@$methodName: query from $caller" }
            HMAServiceCache.instance.putShouldHideUidCache(callingUid, caller, targetApp)
            service?.increasePMFilterCount(caller)
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

        val callingApps = getCallingApps(callingUid)
        val callingUser = getUserFromCallingUid(callingUid)

        val query = findTargetApp() ?: return

        for (caller in callingApps) {
            val isHide = service?.shouldHideInstallationSource(caller, query, callingUser)
                ?: Constants.FAKE_INSTALLATION_SOURCE_DISABLED
            if (isHide == Constants.FAKE_INSTALLATION_SOURCE_DISABLED) continue

            logD(TAG) { "@$methodName: Applied installer hiding for $caller - $callingUid => $isHide" }

            applyReturnValue(isHide)

            service?.increaseInstallerFilterCount(caller)
            break
        }
    }
}
