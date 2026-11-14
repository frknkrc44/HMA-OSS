package org.frknkrc44.hma_oss.zygote.service

import icu.nullptr.hidemyapplist.common.FilterHolder
import icu.nullptr.hidemyapplist.common.PresetCache
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils
import java.util.concurrent.ConcurrentHashMap

class HMAServiceDataHolder {
    private val filterCountLock = Any()

    private data class UidEntry(val caller: String, val queries: MutableSet<String>)

    private val uidHideCache = ConcurrentHashMap<Int, UidEntry>()

    var presetCache = PresetCache()
        internal set

    var filterHolder = FilterHolder()
        internal set

    fun addIntoPresetCache(preset: String, packageName: String): Boolean {
        var returnedValue = false

        returnedValue = returnedValue or (presetCache.cache[preset]?.add(packageName) ?: false)
        if (RiskyPackageUtils.instance.appHasGMSConnection(packageName, true)) {
            returnedValue = returnedValue or presetCache.riskyPackageCache.add(packageName)
        }

        return returnedValue
    }

    fun removeFromPresetCache(preset: String, packageName: String): Boolean {
        var returnedValue = false

        returnedValue = returnedValue or (presetCache.cache[preset]?.remove(packageName) ?: false)
        returnedValue = returnedValue or presetCache.riskyPackageCache.remove(packageName)

        return returnedValue
    }

    fun findCallerByUid(uid: Int) = uidHideCache[uid]?.caller

    fun shouldHideFromUid(uid: Int, query: String?): Boolean? {
        if (query == null) return null

        return uidHideCache[uid]?.queries?.contains(query) == true
    }

    fun putShouldHideUidCache(uid: Int, caller: String, query: String) {
        uidHideCache.computeIfAbsent(uid) {
            UidEntry(caller, ConcurrentHashMap.newKeySet())
        }.queries.add(query)
    }

    fun clearUidCache() = uidHideCache.clear()

    fun increaseFilterCount(
        uid: Int?,
        amount: Int = 1,
        filterType: FilterHolder.FilterType,
        writeFilterCount: () -> Unit,
    ) {
        if (uid == null || amount < 1) return

        val caller = findCallerByUid(uid) ?: return

        return increaseFilterCount(caller, amount, filterType, writeFilterCount)
    }

    fun increaseFilterCount(
        caller: String?,
        amount: Int = 1,
        filterType: FilterHolder.FilterType,
        writeFilterCount: () -> Unit,
    ) {
        if (caller == null || amount < 1) return

        synchronized(filterCountLock) {
            if (!filterHolder.filterCounts.containsKey(caller)) {
                filterHolder.filterCounts[caller] = FilterHolder.FilterCount()
            }

            val filterCount = filterHolder.filterCounts[caller]!!
            when (filterType) {
                FilterHolder.FilterType.PACKAGE_MANAGER -> filterCount.packageManagerCount += amount
                FilterHolder.FilterType.ACTIVITY_LAUNCH -> filterCount.activityLaunchCount += amount
                FilterHolder.FilterType.INSTALLER -> filterCount.installerCount += amount
                FilterHolder.FilterType.SETTINGS -> filterCount.settingsCount += amount
                FilterHolder.FilterType.OTHERS -> filterCount.othersCount += amount
            }
        }

        writeFilterCount()
    }
}
