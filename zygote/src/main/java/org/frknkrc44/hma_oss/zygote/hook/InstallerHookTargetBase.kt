package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageManager
import android.os.Binder
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils.getPackageInfoCompat
import icu.nullptr.hidemyapplist.common.Utils.getUserFromCallingUid
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument

abstract class InstallerHookTargetBase : IFrameworkHook {
    override val TAG = "InstallerHookCommon"

    abstract val fakeSystemPackageInstallSourceInfo: Any?
    abstract val fakeUserPackageInstallSourceInfo: Any?

    protected val psPackageInfo by lazy {
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

    override fun load() {
        logI(TAG) { "Load hook" }

        hooker.apply {
            if (service.pmn != null) {
                hookBefore(
                    service.pmn!!.javaClass.name,
                    "getInstallerForPackage",
                ) { methodName, frame, returnValue ->
                    applyInstallerHiding(
                        methodName,
                        Binder.getCallingUid(),
                        { frame.getArgument(1) as? String },
                    ) {
                        when (it) {
                            Constants.FAKE_INSTALLATION_SOURCE_USER -> returnValue.result = VENDING_PACKAGE_NAME
                            Constants.FAKE_INSTALLATION_SOURCE_SYSTEM -> returnValue.result = "preload"
                        }
                    }
                }
            }
        }
    }


    inline fun applyInstallerHiding(
        methodName: String,
        callingUid: Int,
        findTargetApp: () -> String?,
        applyReturnValue: (Int) -> Unit,
    ) {
        if (callingUid == Constants.UID_SYSTEM) return

        val callingApps = getCallingApps(pms, callingUid)
        val callingUser = getUserFromCallingUid(callingUid)

        val query = findTargetApp() ?: return

        for (caller in callingApps) {
            val isHide = service.shouldHideInstallationSource(caller, query, callingUser)
            if (isHide == Constants.FAKE_INSTALLATION_SOURCE_DISABLED) continue

            logD(TAG) { "@$methodName: Applied installer hiding for $caller - $callingUid => $isHide" }

            applyReturnValue(isHide)

            service.increaseInstallerFilterCount(caller)
            break
        }
    }
}
