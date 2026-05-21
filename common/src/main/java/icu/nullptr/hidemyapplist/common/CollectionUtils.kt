package icu.nullptr.hidemyapplist.common

object CollectionUtils {
    inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
        this.filter { (key, value) -> predicate(key, value) }.forEach { this.remove(it.key) }
    }

    inline fun <K, V> MutableMap<K, V>.removeIfWithCount(predicate: (K, V) -> Boolean): Int {
        return this.filter { (key, value) -> predicate(key, value) }.count { this.remove(it.key) != null }
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
}
