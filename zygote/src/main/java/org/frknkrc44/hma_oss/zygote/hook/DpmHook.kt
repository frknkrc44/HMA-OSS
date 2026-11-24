package org.frknkrc44.hma_oss.zygote.hook

import android.content.ComponentName
import android.os.Binder
import com.v7878.unsafe.invoke.EmulatedStackFrame
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.ReturnValue
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackagesForUidLocal
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.DEVICE_POLICY_MANAGER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.DP_OWNERS_CLASS
import java.util.concurrent.atomic.AtomicReference

class DpmHook : IFrameworkHook {
    override val TAG = "DpmHook"

    private val lastDPMApp: AtomicReference<String?> = AtomicReference(null)

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        hooker.apply {
            hookAfter(
                DEVICE_POLICY_MANAGER_CLASS,
                "getActiveAdmins",
            ) { methodName, _, returnValue ->
                val callingUid = Binder.getCallingUid()
                if (callingUid == Constants.UID_SYSTEM) return@hookAfter

                logD(TAG) { "@$methodName: query received from $callingUid" }

                val result = returnValue.result as? List<ComponentName> ?: return@hookAfter

                val caller = dataHolder.findCallerByUid(callingUid)
                    ?: getPackagesForUidLocal(service.pms, callingUid).firstOrNull {
                    service.isHookEnabled(it)
                } ?: return@hookAfter

                val filtered = result.filter { !service.shouldHide(caller, it.packageName) }

                if (filtered.size != result.size) {
                    val amount = result.size - filtered.size
                    logD(TAG) { "@$methodName: $amount entries filtered for $callingUid ($caller)" }
                    val last = lastDPMApp.getAndSet(caller)
                    if (last != caller) logI(TAG) { "@$methodName: query from $caller" }
                    returnValue.result = filtered
                    (result - filtered.toSet()).forEach {
                        dataHolder.putShouldHideUidCache(callingUid, caller, it.packageName)
                    }
                    service.increasePMFilterCount(caller, amount)
                }
            }

            hookAfter(
                DP_OWNERS_CLASS,
                "getDeviceOwnerComponent",
                hook = ::applyOwnerComponentHiding,
            )

            hookAfter(
                DP_OWNERS_CLASS,
                "getProfileOwnerComponent",
                hook = ::applyOwnerComponentHiding,
            )

            hookAfter(
                DP_OWNERS_CLASS,
                "getDeviceOwnerPackageName",
                hook = ::applyOwnerPackageHiding,
            )

            hookAfter(
                DP_OWNERS_CLASS,
                "getProfileOwnerPackage",
                hook = ::applyOwnerPackageHiding,
            )
        }
    }

    private fun applyOwnerPackageHiding(
        methodName: String,
        @Suppress("unused") frame: EmulatedStackFrame,
        returnValue: ReturnValue,
    ) = applyDPMPackageHiding(
        methodName,
        returnValue
    ) { returnValue.result as? String }

    private fun applyOwnerComponentHiding(
        methodName: String,
        @Suppress("unused") frame: EmulatedStackFrame,
        returnValue: ReturnValue,
    ) = applyDPMPackageHiding(
        methodName,
        returnValue
    ) { (returnValue.result as? ComponentName)?.packageName }

    private inline fun applyDPMPackageHiding(
        methodName: String,
        returnValue: ReturnValue,
        getTargetApp: () -> String?,
    )  {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Constants.UID_SYSTEM) return

        logD(TAG) { "@$methodName: query received from $callingUid" }

        val targetApp = getTargetApp() ?: return

        if (dataHolder.shouldHideFromUid(callingUid, targetApp) == true) {
            returnValue.result = null
            service.increasePMFilterCount(callingUid)
            logD(TAG) { "@$methodName caller cache: $callingUid, target: $targetApp" }
            return
        }

        val caller = getPackagesForUidLocal(service.pms, callingUid).firstOrNull {
            service.shouldHide(it, targetApp)
        }

        if (caller != null) {
            logD(TAG) { "@$methodName: query filtered for $callingUid ($caller)" }
            val last = lastDPMApp.getAndSet(caller)
            if (last != caller) logI(TAG) { "@$methodName: query from $caller" }
            returnValue.result = null
            service.increasePMFilterCount(caller)
            dataHolder.putShouldHideUidCache(callingUid, caller, targetApp)
        }
    }
}
