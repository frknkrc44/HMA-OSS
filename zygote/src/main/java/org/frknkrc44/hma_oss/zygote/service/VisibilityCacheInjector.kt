package org.frknkrc44.hma_oss.zygote.service

import android.os.Build
import androidx.annotation.RequiresApi
import icu.nullptr.hidemyapplist.common.Utils.binderLocalScope
import icu.nullptr.hidemyapplist.common.Utils.getPackageUidCompat
import org.frknkrc44.hma_oss.zygote.util.Logcat.logE
import org.frknkrc44.hma_oss.zygote.util.Logcat.logI
import org.frknkrc44.hma_oss.zygote.util.UserManagerUtils
import java.lang.reflect.Method

/**
 * Direction one: instead of hooking the per-query visibility methods, write the hide
 * decisions into PackageManagerService's own `AppsFilterImpl.mShouldFilterCache` matrix.
 *
 * The system consults that matrix on every `shouldFilterApplication`, and
 * `getPackageInfoInternal` / `getApplicationInfoInternal` all funnel through it, so one
 * data write replaces three hot-path hooks. The query path then carries no HMA code.
 *
 * The system rebuilds the matrix after package and user changes, overwriting our writes,
 * so we re-apply at the end of each rebuild. If the device turned the matrix off
 * (`debug.pm.use_app_filter_cache=false`), the injector reports inactive and the caller
 * keeps the per-query hooks.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object VisibilityCacheInjector {
    private const val TAG = "VisibilityInjector"
    private const val APPS_FILTER_IMPL_CLASS = "com.android.server.pm.AppsFilterImpl"

    private var appsFilter: Any? = null
    private var matrix: Any? = null
    private var matrixPut: Method? = null
    private var cacheLock: Any? = null

    // Caller/target uid pairs written on the last sync, so a config change that unhides an
    // app can tell the system to recompute the pairs it no longer owns.
    @Volatile
    private var lastHidden: Set<Long> = emptySet()

    /** True once the matrix was located and the device keeps it enabled. */
    val active: Boolean get() = matrix != null && matrixPut != null && cacheLock != null

    // The cache fields live on different levels: mShouldFilterCache / mCacheReady /
    // mCacheEnabled on AppsFilterBase, mCacheLock on AppsFilterLocked. Walk the hierarchy.
    private fun rawField(obj: Any, name: String): java.lang.reflect.Field? {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            runCatching { return clazz!!.getDeclaredField(name).apply { isAccessible = true } }
            clazz = clazz.superclass
        }
        return null
    }

    private fun objField(obj: Any, name: String): Any? = rawField(obj, name)?.get(obj)

    private fun boolField(obj: Any, name: String): Boolean = rawField(obj, name)?.getBoolean(obj) ?: false

    fun attach(service: HMAService) {
        val filter = findAppsFilter(service) ?: run {
            logI(TAG) { "AppsFilterImpl not found, keeping per-query hooks" }
            return
        }

        val cache = objField(filter, "mShouldFilterCache") ?: run {
            logI(TAG) { "mShouldFilterCache not found, keeping per-query hooks" }
            return
        }

        if (!boolField(filter, "mCacheEnabled")) {
            logI(TAG) { "Device disabled the visibility cache, keeping per-query hooks" }
            return
        }

        val put = runCatching {
            cache.javaClass.getMethod(
                "put",
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
            )
        }.getOrNull() ?: run {
            logI(TAG) { "Matrix put(int,int,boolean) not found, keeping per-query hooks" }
            return
        }

        appsFilter = filter
        matrix = cache
        matrixPut = put
        cacheLock = objField(filter, "mCacheLock") ?: filter

        logI(TAG) { "Attached to AppsFilterImpl matrix" }
    }

    private fun findAppsFilter(service: HMAService): Any? {
        objField(service.pms, "mAppsFilter")?.let { if (isImpl(it)) return it }
        val pmn = service.pmn ?: return null
        val pm = objField(pmn, "mPm") ?: return null
        return objField(pm, "mAppsFilter")?.takeIf { isImpl(it) }
    }

    private fun isImpl(obj: Any): Boolean {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            if (clazz.name == APPS_FILTER_IMPL_CLASS) return true
            clazz = clazz.superclass
        }
        return false
    }

    /**
     * The system rebuilds the whole matrix after package and user changes and overwrites our
     * writes. Re-apply at the end of each rebuild. The rebuild runs under mCacheLock and
     * releases it before these hooks fire, so syncAll takes the lock fresh.
     */
    fun installReapplyHooks(hooker: BulkHooker) {
        hooker.hookAfter(APPS_FILTER_IMPL_CLASS, "updateEntireShouldFilterCacheInner") { _, _, _ ->
            syncAll()
        }
        hooker.hookAfter(APPS_FILTER_IMPL_CLASS, "updateShouldFilterCacheForPackage", 2) { _, _, _ ->
            syncAll()
        }
    }

    /**
     * Recompute every configured hide pair and write it into the matrix. Called after the
     * initial load, after a config change, and at the end of each system rebuild. Writes
     * made before mCacheReady flips survive the remainder of the build and take effect once
     * the system marks the cache ready; the read path ignores the matrix until then.
     */
    fun syncAll() {
        val cache = matrix ?: return
        val put = matrixPut ?: return
        val lock = cacheLock ?: return

        val pairs = computeHiddenPairs() ?: return

        synchronized(lock) {
            for (packed in pairs) {
                val callerUid = (packed ushr 32).toInt()
                val targetUid = packed.toInt()
                try {
                    put.invoke(cache, callerUid, targetUid, true)
                } catch (cause: Throwable) {
                    logE(TAG, cause) { "Failed to write hide pair $callerUid -> $targetUid" }
                }
            }
        }

        lastHidden = pairs
        logI(TAG) { "Synced ${pairs.size} hide pairs into the visibility matrix" }
    }

    /**
     * Apply a config change. Pairs that are hidden now are written immediately. If the change
     * unhid any pair the system still owns, ask the system to recompute the whole matrix so
     * the baseline value is restored for those pairs; the rebuild re-apply hook then writes
     * the current hides back. Pure additions take effect at once with no rebuild.
     */
    fun onConfigChanged() {
        val pairs = computeHiddenPairs() ?: return
        val removed = lastHidden - pairs
        syncAll()
        if (removed.isNotEmpty()) {
            logI(TAG) { "Config unhid ${removed.size} pairs, requesting a matrix rebuild" }
            requestRebuild()
        }
    }

    private fun computeHiddenPairs(): Set<Long>? {
        val service = UserService.service ?: return null
        val scope = service.config.scope.keys.toList()
        if (scope.isEmpty()) return emptySet()

        // Resolve the hide pairs without holding the cache lock, since shouldHide may do
        // binder work (WebView provider, default browser).
        val pairs = HashSet<Long>(scope.size * 16)
        binderLocalScope {
            val targets = service.pms.allPackages
            for (userId in UserManagerUtils.userIds) {
                for (caller in scope) {
                    val callerUid = runCatching {
                        service.pms.getPackageUidCompat(caller, 0L, userId)
                    }.getOrDefault(-1)
                    if (callerUid < 0) continue

                    for (target in targets) {
                        if (!service.shouldHide(caller, target, userId)) continue
                        val targetUid = runCatching {
                            service.pms.getPackageUidCompat(target, 0L, userId)
                        }.getOrDefault(-1)
                        if (targetUid < 0) continue
                        pairs.add((callerUid.toLong() shl 32) or (targetUid.toLong() and 0xffffffffL))
                    }
                }
            }
        }
        return pairs
    }

    private fun requestRebuild() {
        val filter = appsFilter ?: return
        try {
            val localServices = Class.forName("com.android.server.LocalServices")
            val pmInternalClass = Class.forName("android.content.pm.PackageManagerInternal")
            val pmInternal = localServices
                .getMethod("getService", Class::class.java)
                .invoke(null, pmInternalClass) ?: return

            filter.javaClass
                .getDeclaredMethod("invalidateCache", String::class.java)
                .apply { isAccessible = true }
                .invoke(filter, "HMA config change")

            filter.javaClass
                .getDeclaredMethod(
                    "updateEntireShouldFilterCacheAsync",
                    pmInternalClass,
                    Int::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
                .invoke(filter, pmInternal, 0)
        } catch (cause: Throwable) {
            // The system rebuilds on the next package or user change regardless; until then
            // the unhidden pair stays hidden. Report it rather than hiding the failure.
            logE(TAG, cause) { "Could not request a matrix rebuild; unhide waits for the next system rebuild" }
        }
    }
}
