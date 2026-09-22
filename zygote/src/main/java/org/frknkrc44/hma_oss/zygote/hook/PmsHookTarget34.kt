package org.frknkrc44.hma_oss.zygote.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getCallingApps
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.PACKAGE_MANAGER_SERVICE_CLASS

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PmsHookTarget34 : PmsHookTarget33() {
    override val TAG = "PmsHookTarget34"

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        super.load()

        hooker.apply {
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
                    returnValue,
                    { Binder.getCallingUid() },
                    { frame.getArgument(1) as? String },
                    ::getCallingApps,
                    null,
                )
            }
        }
    }
}
