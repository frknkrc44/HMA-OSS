package org.frknkrc44.hma_oss.zygote.util

object CollectionUtils {
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
