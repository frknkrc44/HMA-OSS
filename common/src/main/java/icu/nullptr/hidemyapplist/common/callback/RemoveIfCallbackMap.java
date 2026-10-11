package icu.nullptr.hidemyapplist.common.callback;

@FunctionalInterface
public interface RemoveIfCallbackMap<K, V> {
    boolean accept(K key, V value);
}
