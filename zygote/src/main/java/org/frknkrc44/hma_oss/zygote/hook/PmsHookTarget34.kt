package org.frknkrc44.hma_oss.zygote.hook

import android.content.pm.PackageInstaller
import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findConstructor
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.findMethod
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArg
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.APPS_FILTER_IMPL_CLASS
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PmsHookTarget34(service: HMAService) : PmsHookTargetBase(service) {

    override val TAG = "PmsHookTarget34"

    private val getPackagesForUidMethod by lazy {
        findMethod(
            "com.android.server.pm.Computer",
            "getPackagesForUid",
            isDeclared = false,
            systemClassLoader = true,
            Int::class.java,
        )
    }

    override val fakeSystemPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            6,
        )!!.newInstance(
            null,
            null,
            null,
            null,
            null,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )
    }

    override val fakeUserPackageInstallSourceInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo",
            6,
        )!!.newInstance(
            VENDING_PACKAGE_NAME,
            psPackageInfo?.signingInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
            PackageInstaller.PACKAGE_SOURCE_STORE,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG) { "Load hook" }

        BulkHooker.instance.apply {
            hookBefore(
                APPS_FILTER_IMPL_CLASS,
                "shouldFilterApplication",
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { frame.getArg(2) as Int? },
                    { getPackageNameFromPackageSettings(frame.getArg(4)) },
                    {
                        Utils.binderLocalScope {
                            getPackagesForUidMethod.invoke(frame.getArg(1), it) as Array<String>?
                        }
                    },
                    { returnValue.result = true },
                )
            }

            // AOSP exploit - https://github.com/aosp-mirror/platform_frameworks_base/commit/5bc482bd99ea18fe0b4064d486b29d5ae2d65139
            // Only 14 QPR2+ has this method
            // UPDATE: Samsung adds getArchivedPackage instead of getArchivedPackageInternal
            val altNames = findAltMethod(
                listOf(PACKAGE_MANAGER_SERVICE_CLASS),
                listOf("getArchivedPackageInternal", "getArchivedPackage"),
            ) ?: return@apply

            hookBefore(
                altNames.declaringClass.name,
                altNames.name,
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    { Binder.getCallingUid() },
                    { frame.getArg(1) as? String },
                    { getCallingApps(service, it) },
                    { returnValue.result = null },
                )
            }
        }

        super.load()
    }
}
