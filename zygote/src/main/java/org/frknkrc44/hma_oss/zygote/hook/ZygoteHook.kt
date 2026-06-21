package org.frknkrc44.hma_oss.zygote.hook

import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService.Companion.service
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.setArg
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS

class ZygoteHook : IFrameworkHook {
    override val TAG = "ZygoteHook"

    override fun load() {
        BulkHooker.instance.hookBefore(
            ZYGOTE_PROCESS_CLASS,
            "start",
        ) { _, frame, _ ->
            logD(TAG) { "@startZygoteProcess: Starting ${frame.args.contentToString()}" }

            // ignore if the GIDs array is null
            val gIDsIndex = frame.args.indexOfFirst { it is IntArray }
            if (gIDsIndex < 0) return@hookBefore

            val caller = frame.args.lastOrNullWithType<String>() ?: return@hookBefore
            var perms = service?.getRestrictedZygotePermissions(caller) ?: return@hookBefore
            if (perms.isNotEmpty()) {
                val gIDs = frame.args[gIDsIndex] as IntArray

                // add more security, reject if not available in GID_PAIRS
                perms = perms.filter { Constants.GID_PAIRS.containsValue(it) }

                logD(TAG) { "@startZygoteProcess: GIDs are ${gIDs.contentToString()}, removing $perms now" }
                frame.setArg(gIDsIndex, gIDs.filter { it !in perms }.toIntArray())
                service?.increaseOthersFilterCount(caller)
            }
        }
    }
}
