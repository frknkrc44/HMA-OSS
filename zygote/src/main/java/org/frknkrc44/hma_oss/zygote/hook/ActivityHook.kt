package org.frknkrc44.hma_oss.zygote.hook

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstWithType
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_STACK_SUPERVISOR_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_STARTER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_TASK_SUPERVISOR_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

class ActivityHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "ActivityHook"

    companion object {
        private val fakeReturnCode by lazy {
            getStaticIntField(
                "android.app.ActivityManager",
                "START_CLASS_NOT_FOUND",
            )
        }
    }

    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hookBefore(
                    ACTIVITY_STARTER_CLASS,
                    "executeRequest",
                ) { param ->
                    val request = param.getArgument(1)
                    val caller = getObjectField(request, "callingPackage") as? String ?: return@hookBefore
                    val intent = getObjectField(request, "intent") as? Intent ?: return@hookBefore
                    val targetApp = intent.component?.packageName

                    if (service.shouldHideActivityLaunch(caller, targetApp)) {
                        logD(TAG) { "@executeRequest: insecure query from $caller, target: ${intent.component}" }
                        param.result = fakeReturnCode
                        service.increaseALFilterCount(caller)
                    }
                }
            } else {
                hookBefore(
                    ACTIVITY_STARTER_CLASS,
                    "startActivity",
                ) { param ->
                    val caller = param.args.firstOrNullWithType<String>() ?: return@hookBefore
                    val intent = param.args.firstOrNullWithType<Intent>() ?: return@hookBefore
                    val targetApp = intent.component?.packageName

                    if (service.shouldHideActivityLaunch(caller, targetApp)) {
                        logD(TAG) { "@startActivity: insecure query from $caller, target: ${intent.component}" }
                        param.result = fakeReturnCode
                        service.increaseALFilterCount(caller)
                    }
                }
            }

            hookBefore(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ACTIVITY_TASK_SUPERVISOR_CLASS
                } else {
                    ACTIVITY_STACK_SUPERVISOR_CLASS
                },
                "checkStartAnyActivityPermission",
            ) { param ->
                logV(TAG) { "${param.methodName}: ${param.args.contentToString()}" }

                // just an empty hook that does nothing
            }

            if (!OSUtils.isSamsung()) {
                hookBefore(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        COMPUTER_ENGINE_CLASS
                    } else {
                        PACKAGE_MANAGER_SERVICE_CLASS
                    },
                    "applyPostResolutionFilter",
                ) { param ->
                    @Suppress("UNCHECKED_CAST") // I know what I do
                    val list = param.args[1] as List<ResolveInfo>?
                    if (list.isNullOrEmpty()) return@hookBefore

                    val callingUid = param.args.firstWithType<Int>()
                    if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                    val callingApps = getCallingApps(service, callingUid)
                    val caller = callingApps.firstOrNull { service.isHookEnabled(it) }
                    if (caller != null) {
                        logV(TAG) { "@${param.methodName}: $caller requested a resolve info" }

                        val filteredList = list.filter { resolveInfo ->
                            val targetApp = Utils.getPackageNameFromResolveInfo(resolveInfo)

                            logV(TAG) { "@${param.methodName}: Checking $targetApp for $caller" }

                            (!service.shouldHideActivityLaunch(caller, targetApp)).apply {
                                if (!this) {
                                    logD(TAG) { "@${param.methodName}: Filtered $targetApp from $caller" }
                                }
                            }
                        }

                        if (filteredList.size != list.size) {
                            param.setArgument(1, filteredList.toList())

                            service.increasePMFilterCount(caller)
                        }
                    }
                }
            }
        }
    }
}
