package org.frknkrc44.hma_oss.zygote.hook

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodSubtype
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.common.settings_presets.InputMethodPreset
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.service.HookParam
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logV
import org.frknkrc44.hma_oss.zygote.util.Logcat.logW
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageManager
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.callStaticMethod
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.IMM_IMPL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.IMM_SERVICE_CLASS
import java.util.Collections

class ImmHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "ImmHook"

    // TODO: Find a method to get settings activity
    fun getFakeInputMethodInfo(packageName: String): InputMethodInfo {
        val defaultInputMethod = service.getSpoofedSetting(
            packageName,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            Constants.SETTINGS_SECURE,
        )

        if (defaultInputMethod?.value != null) {
            try {
                val component = ComponentName.unflattenFromString(defaultInputMethod.value!!)!!
                logD(TAG) { "Package component: \"$component\"" }

                val pkgManager = getPackageManager()
                val kbdPackage = Utils.binderLocalScope {
                    pkgManager.getApplicationInfo(component.packageName, 0)
                }

                return InputMethodInfo(
                    component.packageName,
                    component.className,
                    kbdPackage.loadLabel(pkgManager),
                    null,
                )
            } catch (e: Throwable) {
                logV(TAG, e) { e.message ?: "" }
            }
        }

        return InputMethodInfo(
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin.LatinIME",
            "Gboard",
            null,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        // OEMs (especially Samsung and Xiaomi) messes up whole framework code,
        // so nothing left except messing up this code
        BulkHooker.instance.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                findAltMethod(
                    listOf(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                    listOf("getCurrentInputMethodInfoAsUser"),
                )?.let { method ->
                    hookBefore(
                        method.declaringClass.name,
                        method.name,
                    ) { param ->
                        val callingApps = getCallingApps(service)

                        val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
                        if (caller != null) {
                            logD(TAG) { "@${param.methodName} spoofed input method for $caller" }

                            val fakeIMInfo = getFakeInputMethodInfo(caller)
                            val userHandle = param.getArgument(1) as Int
                            if (!isIMExists(fakeIMInfo.packageName, userHandle)) {
                                warnNotInstalledKeyboard(param.methodName, fakeIMInfo.packageName)
                            }

                            param.result = fakeIMInfo
                            service.increaseSettingsFilterCount(caller)
                        }
                    }
                }
            }

            findAltMethod(
                listOf(IMM_SERVICE_CLASS),
                listOf("getInputMethodList", "getInputMethodListInternal"),
            )?.let { method ->
                hookAfter(
                    method.declaringClass.name,
                    method.name,
                ) { param ->
                    logD(TAG) { "@${param.methodName}: hook init" }

                    val currentResult = param.result ?: return@hookAfter
                    logD(TAG) { "@${param.methodName}: Result: $currentResult Args: ${param.args.contentToString()}" }

                    val callingUid = if (param.args.count { it is Int } > 2) {
                        param.args.lastOrNull { it is Int && it > 999 } as? Int ?: return@hookAfter
                    } else {
                        Binder.getCallingUid()
                    }

                    logD(TAG) { "@${param.methodName}: Caller ID: $callingUid" }

                    val returnType = param.returnType
                    if (returnType.simpleName == "InputMethodInfoSafeList") {
                        val inList = callStaticMethod(
                            currentResult.javaClass,
                            "extractFrom",
                            currentResult
                        ) as List<InputMethodInfo>

                        val newImmList = calculateReturnedInputMethodList(callingUid, inList)

                        param.result = returnType.getDeclaredMethod(
                            "create",
                            List::class.java,
                        ).apply { isAccessible = true }.invoke(null, newImmList)
                    } else {
                        param.result = calculateReturnedInputMethodList(
                            callingUid, currentResult as List<InputMethodInfo>)
                    }
                }
            }

            findAltMethod(
                listOf(IMM_SERVICE_CLASS),
                listOf("getEnabledInputMethodList", "getEnabledInputMethodListInternal"),
            )?.let { method ->
                hookBefore(
                    method.declaringClass.name,
                    method.name,
                ) { param ->
                    val callingApps = getCallingApps(service)

                    val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
                    if (caller != null) {
                        logD(TAG) { "@${param.methodName}: spoofed input method for $caller" }

                        val fakeIMInfo = getFakeInputMethodInfo(caller)
                        if (!isIMExists(fakeIMInfo.packageName)) {
                            warnNotInstalledKeyboard(param.methodName, fakeIMInfo.packageName)
                        }

                        listOf(fakeIMInfo).let { list ->
                            val returnType = param.returnType
                            param.result = if (returnType.simpleName == "InputMethodInfoSafeList") {
                                returnType.getDeclaredMethod(
                                    "create",
                                    List::class.java,
                                ).apply { isAccessible = true }.invoke(null, list)
                            } else { list }
                        }

                        service.increaseSettingsFilterCount(caller)
                    }
                }
            }

            findAltMethod(
                listOf(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                listOf("getCurrentInputMethodSubtype"),
            )?.let {
                hookBefore(
                    it.declaringClass.name,
                    it.name,
                ) { param ->
                    subtypeHook(param)
                }
            }

            findAltMethod(
                listOf(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                listOf("getLastInputMethodSubtype"),
            )?.let {
                hookBefore(
                    it.declaringClass.name,
                    it.name,
                ) { param ->
                    subtypeHook(param)
                }
            }

            findAltMethod(
                listOf(IMM_SERVICE_CLASS, IMM_IMPL_CLASS),
                listOf("getEnabledInputMethodSubtypeListInternal", "getEnabledInputMethodSubtypeList")
            )?.let {
                hookBefore(
                    it.declaringClass.name,
                    it.name,
                ) { param ->
                    subtypeListHook(param)
                }
            }
        }
    }

    private fun subtypeHook(param: HookParam) {
        val callingApps = getCallingApps(service)

        val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
        if (caller != null) {
            logD(TAG) { "@${param.methodName}: spoofed input method subtype for ${callingApps.contentToString()}" }

            // TODO: Find a method to get exact value for spoofed input method
            param.result = null
            service.increaseSettingsFilterCount(caller)
        }
    }

    private fun subtypeListHook(param: HookParam) {
        val callingApps = getCallingApps(service)

        val caller = callingApps.firstOrNull { callerIsSpoofed(it) }
        if (caller != null) {
            logD(TAG) { "@${param.methodName}: spoofed input method subtype for ${callingApps.contentToString()}" }

            // TODO: Find a method to get exact list for spoofed input method
            Collections.emptyList<InputMethodSubtype>().let { list ->
                val returnType = param.returnType
                param.result = if (returnType.simpleName == "InputMethodSubtypeSafeList") {
                    returnType.getDeclaredMethod(
                        "create",
                        List::class.java,
                    ).apply { isAccessible = true }.invoke(null, list)
                } else { list }
            }

            service.increaseSettingsFilterCount(caller)
        }
    }

    fun calculateReturnedInputMethodList(callingUid: Int, inList: List<InputMethodInfo>): List<InputMethodInfo> {
        logV(TAG) { "@getInputMethodList*calculator: $callingUid - Current: ${inList.map { it.component }}" }

        val caller = getCallingApps(service, callingUid)
            .firstOrNull { callerIsSpoofed(it) } ?: return inList

        logD(TAG) { "@getInputMethodList: spoofed input method for $caller" }

        val calculatedList = inList.filter { imInfo ->
            !service.shouldHide(caller, imInfo.packageName)
        }

        logV(TAG) { "@getInputMethodList*calculator: $callingUid - Calculated: ${calculatedList.map { it.component }}" }

        val fakeIMInfo = getFakeInputMethodInfo(caller)
        val imExists = isIMExists(fakeIMInfo.packageName)
        if (!(imExists && calculatedList.any { it.packageName == fakeIMInfo.packageName })) {
            if (!imExists) {
                warnNotInstalledKeyboard("getInputMethodList*calculator", fakeIMInfo.packageName)
            }

            if (!calculatedList.any { it.packageName == fakeIMInfo.packageName }) {
                return (calculatedList + fakeIMInfo).sortedWith { info1, info2 ->
                    info1.packageName.compareTo(info2.packageName)
                }
            }
        }

        return calculatedList
    }

    private fun isIMExists(packageName: String, inUserId: Int? = null): Boolean {
        if (packageName in service.systemApps) return true

        val userId = inUserId ?: Binder.getCallingUserHandle().hashCode()
        return Utils.binderLocalScope {
            Utils.getPackageUidCompat(service.pms, packageName, PackageManager.MATCH_ALL.toLong(), userId) >= 0
        }
    }

    private fun warnNotInstalledKeyboard(methodName: String, packageName: String) {
        logW(TAG) { "@$methodName: PROBABLY spoofing for a not installed keyboard, please install $packageName or spoof for another keyboard by using settings templates to reduce detections. Do not care this message if you are sure the keyboard is installed correctly." }
    }

    private fun callerIsSpoofed(caller: String) =
        service.getEnabledSettingsPresets(caller).contains(InputMethodPreset.NAME)
}
