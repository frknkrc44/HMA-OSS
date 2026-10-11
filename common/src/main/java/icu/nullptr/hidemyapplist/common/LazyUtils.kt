package icu.nullptr.hidemyapplist.common

import java.util.WeakHashMap
import kotlin.reflect.KProperty

// Credits: https://stackoverflow.com/a/38084930/16290110
fun <T, R> lazyWithReceiver(initializer: T.() -> R): LazyWithReceiver<T, R> = LazyWithReceiver(initializer)

class LazyWithReceiver<T, out R>(val initializer: T.() -> R) {
    private val values = WeakHashMap<T, R>()

    operator fun getValue(thisRef: T, property: KProperty<*>): R = synchronized(values) {
        return values.getOrPut(thisRef) { thisRef.initializer() }
    }
}
