package icu.nullptr.hidemyapplist.common

object CollectionUtils {
    inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
        this.filter { (key, value) -> predicate(key, value) }.forEach { this.remove(it.key) }
    }

    inline fun <K, V> MutableMap<K, V>.removeIfWithCount(predicate: (K, V) -> Boolean): Int {
        return this.filter { (key, value) -> predicate(key, value) }.count { this.remove(it.key) != null }
    }

    inline fun <K> MutableSet<K>.removeIfWithCount(predicate: (K) -> Boolean): Int {
        return this.filter { key -> predicate(key) }.count { this.remove(it) }
    }

    inline fun <reified T> Array<*>.firstWithType(): T {
        return this.first { it is T } as T
    }

    inline fun <reified T> Array<*>.firstOrNullWithType(): T? {
        return this.firstOrNull { it is T } as? T
    }

    inline fun <reified T> Array<*>.lastWithType(): T {
        return this.last { it is T } as T
    }

    inline fun <reified T> Array<*>.lastOrNullWithType(): T? {
        return this.lastOrNull { it is T } as? T
    }

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
}
