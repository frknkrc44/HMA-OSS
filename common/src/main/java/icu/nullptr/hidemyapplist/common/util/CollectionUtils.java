package icu.nullptr.hidemyapplist.common.util;

import java.util.Map;
import java.util.NoSuchElementException;

public class CollectionUtils {
    private CollectionUtils() {}

    public static <T> T firstWithType(Object[] items, Class<? extends T> clazz) {
        return firstWithType(items, clazz, false);
    }

    public static <T> T firstOrNullWithType(Object[] items, Class<? extends T> clazz) {
        return firstWithType(items, clazz, true);
    }

    public static <T> T lastWithType(Object[] items, Class<? extends T> clazz) {
        return lastWithType(items, clazz, false);
    }

    public static <T> T lastOrNullWithType(Object[] items, Class<? extends T> clazz) {
        return lastWithType(items, clazz, true);
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    private static <T> T firstWithType(Object[] items, Class<? extends T> clazz, boolean ignoreNotFound) {
        clazz = clazz.isPrimitive() ? (Class<? extends T>) PRIMITIVE_MAP.get(clazz) : clazz;

        for (Object item : items) {
            if (clazz.isInstance(item)) {
                return clazz.cast(item);
            }
        }

        if (!ignoreNotFound) {
            throw new NoSuchElementException("We cannot find a matching element with T type");
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    private static <T> T lastWithType(Object[] items, Class<? extends T> clazz, boolean ignoreNotFound) {
        clazz = clazz.isPrimitive() ? (Class<? extends T>) PRIMITIVE_MAP.get(clazz) : clazz;

        for (int i = items.length - 1; i >= 0; i--) {
            final var item = items[i];

            if (clazz.isInstance(item)) {
                return clazz.cast(item);
            }
        }

        if (!ignoreNotFound) {
            throw new NoSuchElementException("We cannot find a matching element with T type");
        }

        return null;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            long.class, Long.class,
            short.class, Short.class
    );
}
