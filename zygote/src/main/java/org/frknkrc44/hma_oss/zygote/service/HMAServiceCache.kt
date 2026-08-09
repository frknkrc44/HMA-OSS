package org.frknkrc44.hma_oss.zygote.service

import icu.nullptr.hidemyapplist.common.PresetCache

class HMAServiceCache private constructor() {
    companion object {
        val instance by lazy { HMAServiceCache() }
    }

    private val uidHideCache = mutableListOf<Triple<Int, String, MutableList<String>>>()

    var presetCache = PresetCache()
        internal set

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
