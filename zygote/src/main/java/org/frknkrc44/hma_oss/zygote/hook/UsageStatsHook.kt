package org.frknkrc44.hma_oss.zygote.hook

import android.app.usage.UsageStats
import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.Build
import org.frknkrc44.hma_oss.zygote.util.CollectionUtils.lastOrNullWithType
import org.frknkrc44.hma_oss.zygote.util.CollectionUtils.lastWithType
import org.frknkrc44.hma_oss.zygote.util.Logcat.logD
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.ZLUtils.args
import org.frknkrc44.hma_oss.zygote.util.ZygoteConstants.USAGE_STATS_BINDER_SERVICE_CLASS
import java.util.concurrent.atomic.AtomicReference

class UsageStatsHook : IFrameworkHook {
    override val TAG = "UsageStatsHook"

    @PublishedApi
    internal val lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    override fun load() {
        logI(TAG) { "Load hook" }

        hooker.apply {
            hookAfter(
                USAGE_STATS_BINDER_SERVICE_CLASS,
                "queryUsageStats",
            ) { methodName, frame, returnValue ->
                @Suppress("UNCHECKED_CAST")
                val result = returnValue.result as? ParceledListSlice<UsageStats> ?: return@hookAfter

                val args = frame.args
                val caller = args.lastOrNullWithType<String>() ?: return@hookAfter
                val userId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    args.lastWithType<Int>()
                } else {
                    Binder.getCallingUserHandle().hashCode()
                }

                logD(TAG) { "@$methodName: $caller requested stats" }

                val filtered = result.list.filterTo(mutableListOf()) {
                    !service.shouldHide(caller, it.packageName, userId)
                }

                val count = result.list.size - filtered.size
                if (count >= 0) {
                    val last = lastFilteredApp.getAndSet(caller)
                    if (last != caller) logI(TAG) { "@$methodName: query from $caller" }
                    logD(TAG) { "@$methodName: $count entries hidden from $caller" }

                    service.increasePMFilterCount(caller, count)

                    returnValue.result = ParceledListSlice(filtered)
                }
            }
        }
    }
}
