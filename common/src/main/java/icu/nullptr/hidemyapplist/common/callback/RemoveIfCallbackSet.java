package icu.nullptr.hidemyapplist.common.callback;

@FunctionalInterface
public interface RemoveIfCallbackSet<K> {
    boolean accept(K key);
}
