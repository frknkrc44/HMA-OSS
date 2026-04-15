package org.frknkrc44.hma_oss.zygote.hook

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.ParceledListSlice
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.common.settings_presets.AccessibilityPreset
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.Utils4Zygote
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACCESSIBILITY_SERVICE_CLASS

class AccessibilityHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "AccessibilityHook"

    override fun load() {
        BulkHooker.instance.apply {
            @Suppress("UNCHECKED_CAST")
            hookAfter(
                ACCESSIBILITY_SERVICE_CLASS,
                "getInstalledAccessibilityServiceList",
            ) { param ->
                val callingApps = Utils4Zygote.getCallingApps(service)
                if (callingApps.isEmpty()) return@hookAfter

                val currentResult = param.result ?: return@hookAfter
                val returnsParcel = "Parcel" in  param.returnType.simpleName
                val inList = if (returnsParcel) {
                    (currentResult as ParceledListSlice<AccessibilityServiceInfo>).list
                } else {
                    currentResult as List<AccessibilityServiceInfo>
                }

                val calculatedList = calculateReturnedAccessibilityList(callingApps, inList)
                param.result = if (returnsParcel) {
                    ParceledListSlice(calculatedList)
                } else {
                    calculatedList
                }
            }

            hookBefore(
                ACCESSIBILITY_SERVICE_CLASS,
                "getEnabledAccessibilityServiceList",
            ) { param ->
                val callingApps = Utils4Zygote.getCallingApps(service)
                if (callingApps.isEmpty()) return@hookBefore

                val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
                if (caller != null) {
                    logD(TAG) { "@${param.methodName} returning empty list for ${callingApps.contentToString()}" }

                    val returnedList = java.util.ArrayList<AccessibilityServiceInfo>()
                    param.result = if ("Parcel" in param.returnType.simpleName) {
                        ParceledListSlice(returnedList)
                    } else {
                        returnedList
                    }
                }
            }

            hookBefore(
                ACCESSIBILITY_SERVICE_CLASS,
                "addClient",
            ) { param ->
                val callingApps = Utils4Zygote.getCallingApps(service)
                if (callingApps.isEmpty()) return@hookBefore

                val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
                if (caller != null) {
                    param.result = 0L
                    // service.increasePMFilterCount(caller)
                }
            }
        }
    }

    private fun calculateReturnedAccessibilityList(
        callingApps: Array<String>,
        inList: List<AccessibilityServiceInfo>,
    ): List<AccessibilityServiceInfo> {
        logV(TAG) { "@getInstalledAccessibilityServiceList*calculator: $callingApps - Current: $inList" }

        val caller = callingApps.firstOrNull { service.isHookEnabled(it) } ?: return inList

        val calculatedList = inList.filter { asInfo ->
            !service.shouldHide(caller, Utils.getPackageNameFromResolveInfo(asInfo.resolveInfo))
        }

        logV(TAG) { "@getInstalledAccessibilityServiceList*calculator: $caller - Calculated: $calculatedList" }

        return calculatedList
    }

    private fun callerIsSpoofed(caller: String) =
        service.getEnabledSettingsPresets(caller).contains(AccessibilityPreset.NAME)
}
