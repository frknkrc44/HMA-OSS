package org.frknkrc44.hma_oss.zygote.hook

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstOrNullWithType
import icu.nullptr.hidemyapplist.common.CollectionUtils.firstWithType
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.OSUtils
import icu.nullptr.hidemyapplist.common.Utils.getPackageName
import icu.nullptr.hidemyapplist.common.Utils.getPackageUidCompat
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getIntField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getObjectField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getStaticIntField
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArgument
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.thisObject
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_STACK_SUPERVISOR_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_STARTER_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ACTIVITY_TASK_SUPERVISOR_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.COMPUTER_ENGINE_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

class ActivityHook : IFrameworkHook {
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
            hookBefore(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ACTIVITY_TASK_SUPERVISOR_CLASS
                } else {
                    ACTIVITY_STACK_SUPERVISOR_CLASS
                },
                "checkStartAnyActivityPermission",
            ) { methodName, frame, _ ->
                logV(TAG) { "$methodName: ${frame.args.contentToString()}" }

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
                ) { methodName, frame, _ ->
                    @Suppress("UNCHECKED_CAST") // I know what I do
                    val list = frame.args[1] as List<ResolveInfo>?
                    if (list.isNullOrEmpty()) return@hookBefore

                    val callingUid = frame.args.firstWithType<Int>()
                    if (callingUid == Constants.UID_SYSTEM) return@hookBefore

                    val callingUserId = getUserFromCallingUid(callingUid)
                    val callingApps = getCallingApps(callingUid)
                    val caller = callingApps.firstOrNull { service?.isHookEnabled(it) ?: false }
                    if (caller != null) {
                        logV(TAG) { "@$methodName: $caller requested a resolve info" }

                        val filteredList = list.filter { resolveInfo ->
                            val targetApp = resolveInfo.getPackageName()

                            logV(TAG) { "@$methodName: Checking $targetApp for $caller" }

                            (!(service?.shouldHideActivityLaunch(caller, targetApp, callingUserId) ?: false)).apply {
                                if (!this) {
                                    logD(TAG) { "@$methodName: insecure query from $caller, target: $targetApp" }
                                }
                            }
                        }

                        if (filteredList.size != list.size) {
                            frame.setArgument(1, filteredList.toList())

                            service?.increasePMFilterCount(caller, list.size - filteredList.size)
                        }
                    }
                }
            }

            val hookedClazz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                COMPUTER_ENGINE_CLASS
            } else {
                PACKAGE_MANAGER_SERVICE_CLASS
            }

            if (!isHookAvailable(hookedClazz, "applyPostResolutionFilter")) {
                // Try to keep compatibility when InxLocker detected
                val isInxLockerAvailable = service != null && service!!.pms.getPackageUidCompat(
                    "io.github.chimio.inxlocker", 0, 0
                ) >= 0

                if (isInxLockerAvailable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        hookBefore(
                            ACTIVITY_STARTER_CLASS,
                            "executeRequest",
                        ) { _, frame, returnValue ->
                            val request = frame.getArgument(1)
                            val callingUserId = getUserFromCallingUid(getIntField(request, "callingUid"))
                            val caller = getObjectField(request, "callingPackage") as? String ?: return@hookBefore
                            val intent = getObjectField(request, "intent") as? Intent ?: return@hookBefore
                            val targetApp = intent.component?.packageName

                            if (service?.shouldHideActivityLaunch(caller, targetApp, callingUserId) ?: false) {
                                logD(TAG) { "@executeRequest: insecure query from $caller, target: ${intent.component}" }
                                returnValue.result = fakeReturnCode
                                service?.increaseALFilterCount(caller)
                            }
                        }
                    } else {
                        hookBefore(
                            ACTIVITY_STARTER_CLASS,
                            "startActivity",
                        ) { _, frame, returnValue ->
                            // we have no way other than hardcoding, it is 13th argument in AOSP code
                            val callingUserId = getUserFromCallingUid(frame.getArgument(13) as Int)
                            val caller = frame.args.firstOrNullWithType<String>() ?: return@hookBefore
                            val intent = frame.args.firstOrNullWithType<Intent>() ?: return@hookBefore
                            val targetApp = intent.component?.packageName

                            if (service?.shouldHideActivityLaunch(caller, targetApp, callingUserId) ?: false) {
                                logD(TAG) { "@startActivity: insecure query from $caller, target: ${intent.component}" }
                                returnValue.result = fakeReturnCode
                                service?.increaseALFilterCount(caller)
                            }
                        }
                    }
                } else {
                    hookBefore(
                        ACTIVITY_STARTER_CLASS,
                        "execute",
                    ) { _, frame, returnValue ->
                        val request = getObjectField(frame.thisObject, "mRequest") ?: return@hookBefore
                        val callingUserId = getUserFromCallingUid(getIntField(request, "callingUid"))
                        val caller = getObjectField(request, "callingPackage") as? String ?: return@hookBefore
                        val intent = getObjectField(request, "intent") as? Intent ?: return@hookBefore
                        val targetApp = intent.component?.packageName

                        if (service?.shouldHideActivityLaunch(caller, targetApp, callingUserId) ?: false) {
                            logD(TAG) { "@executeRequest: insecure query from $caller, target: ${intent.component}" }
                            returnValue.result = fakeReturnCode
                            service?.increaseALFilterCount(caller)
                        }
                    }
                }
            }
        }
    }
}
