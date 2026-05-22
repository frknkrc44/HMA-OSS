package org.frknkrc44.hma_oss.zygote.hook

import icu.nullptr.hidemyapplist.common.CollectionUtils.lastOrNullWithType
import icu.nullptr.hidemyapplist.common.Constants
import org.frknkrc44.hma_oss.zygote.service.BulkHooker
import org.frknkrc44.hma_oss.zygote.service.HMAService
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS

class ZygoteHook(private val service: HMAService) : IFrameworkHook {
    override val TAG = "ZygoteHook"

    override fun load() {
        BulkHooker.instance.hookBefore(
            ZYGOTE_PROCESS_CLASS,
            "start",
        ) { param ->
            logD(TAG) { "@startZygoteProcess: Starting ${param.args.contentToString()}" }

            // ignore if the GIDs array is null
            val gIDsIndex = param.args.indexOfFirst { it is IntArray }
            if (gIDsIndex < 0) return@hookBefore

            val caller = param.args.lastOrNullWithType<String>() ?: return@hookBefore
            var perms = service.getRestrictedZygotePermissions(caller) ?: return@hookBefore
            if (perms.isNotEmpty()) {
                val gIDs = param.args[gIDsIndex] as IntArray

                // add more security, reject if not available in GID_PAIRS
                perms = perms.filter { Constants.GID_PAIRS.containsValue(it) }

                logD(TAG) { "@startZygoteProcess: GIDs are ${gIDs.contentToString()}, removing $perms now" }
                param.setArgument(gIDsIndex, gIDs.filter { it !in perms }.toIntArray())
                service.increaseOthersFilterCount(caller)
            }
        }
    }
}
