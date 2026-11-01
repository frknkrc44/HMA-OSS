package org.frknkrc44.hma_oss.zygote.hook

import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackageNameFromPackageSettings
import org.frknkrc44.hma_oss.zygote.util.ServiceUtils.getPackagesForUidLocal
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.getArgument

class PmsHookTarget29 : PmsHookTarget29N30() {
    override val TAG = "PmsHookTarget29"

    override fun load() {
        super.load()

        hooker.apply {
            hookBefore(
                service.pms::class.java.name,
                "filterAppAccessLPr",
                argumentCount = 5,
            ) { methodName, frame, returnValue ->
                applyPackageHiding(
                    methodName,
                    returnValue,
                    { frame.getArgument(2) as? Int },
                    { getPackageNameFromPackageSettings(frame.getArgument(1)) },
                    ::getPackagesForUidLocal,
                    true,
                )
            }
        }
    }
}
