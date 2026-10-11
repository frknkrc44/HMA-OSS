package org.frknkrc44.hma_oss.util

object CollectionUtils {

    inline fun <reified T> MutableList<T>.sync(elements: Array<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified T> MutableList<T>.sync(elements: Iterable<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified T> MutableSet<T>.sync(elements: Iterable<T>) {
        clear()
        addAll(elements)
    }

    inline fun <reified K, reified V> MutableMap<K, V>.sync(from: Map<K, V>) {
        clear()
        putAll(from)
    }

    inline fun <reified T> HashSet<T>?.contains(value: T) = this?.contains(value) ?: false
}
