package org.frknkrc44.hma_oss.zygote.service

import icu.nullptr.hidemyapplist.common.PresetCache
import icu.nullptr.hidemyapplist.common.RiskyPackageUtils

class HMAServiceCache private constructor() {
    companion object {
        val instance by lazy { HMAServiceCache() }
    }

    private val uidHideCache = mutableListOf<Triple<Int, String, MutableList<String>>>()

    var presetCache = PresetCache()
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

    fun findCallerByUid(uid: Int) = uidHideCache.firstOrNull { it.first == uid }?.second

    fun shouldHideFromUid(uid: Int, query: String?): Boolean? {
        if (query == null) return null

        return uidHideCache.firstOrNull { it.first == uid && it.third.contains(query) } != null
    }

    fun putShouldHideUidCache(uid: Int, caller: String, query: String) {
        val findList = uidHideCache.firstOrNull { it.first == uid }
        if (findList != null) {
            findList.third.add(query)
        } else {
            uidHideCache.add(Triple(uid, caller, mutableListOf(query)))
        }
    }

    fun clearUidCache() = uidHideCache.clear()
}
