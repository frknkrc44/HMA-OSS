package org.frknkrc44.hma_oss.zygote.util;

import static com.v7878.unsafe.Reflection.getDeclaredField;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.v7878.unsafe.invoke.EmulatedStackFrame;

import org.frknkrc44.hma_oss.zygote.ZygoteEntry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class ZLUtils {
    private ZLUtils() {}

    public static final int PARAMETER_COUNT_UNKNOWN = -1;

    public static Class<?> getReturnType(EmulatedStackFrame frame) {
        return frame.type().returnType();
    }

    @NonNull
    public static Object getThisObject(EmulatedStackFrame frame) {
        return Objects.requireNonNull(getArgument(frame, 0));
    }

    public static Object[] dumpArgs(EmulatedStackFrame frame, boolean skipFirst) {
        final var paramCount = frame.type().parameterCount();
        final var skipCount = skipFirst ? 1 : 0;
        final var objects = new Object[paramCount - skipCount];
        for (int i = skipCount; i < paramCount; i++) {
            objects[i - skipCount] = getArgument(frame, i);
        }
        return objects;
    }

    public static Class<?>[] dumpArgTypes(EmulatedStackFrame frame, boolean skipFirst) {
        final var paramCount = frame.type().parameterCount();
        final var skipCount = skipFirst ? 1 : 0;
        final var objects = new Class<?>[paramCount - skipCount];
        for (int i = skipCount; i < paramCount; i++) {
            objects[i - skipCount] = getArgumentType(frame, i);
        }
        return objects;
    }

    public static Class<?> getArgumentType(EmulatedStackFrame frame, int index) {
        return frame.accessor().getArgumentType(index);
    }

    @Nullable
    public static Object getArgument(EmulatedStackFrame frame, int index) {
        final var accessor = frame.accessor();

        return switch (accessor.getArgumentShorty(index)) {
            case 'L' -> accessor.getReference(index);
            case 'Z' -> accessor.getBoolean(index);
            case 'B' -> accessor.getByte(index);
            case 'C' -> accessor.getChar(index);
            case 'S' -> accessor.getShort(index);
            case 'I' -> accessor.getInt(index);
            case 'J' -> accessor.getLong(index);
            case 'F' -> accessor.getFloat(index);
            case 'D' -> accessor.getDouble(index);
            default -> throw new RuntimeException("Should not reach here");
        };
    }

    public static void setArgument(EmulatedStackFrame frame, int index, Object value) {
        final var accessor = frame.accessor();

        switch (accessor.getArgumentShorty(index)) {
            case 'L' -> accessor.setReference(index, value);
            case 'Z' -> accessor.setBoolean(index, (boolean) value);
            case 'B' -> accessor.setByte(index, (byte) value);
            case 'C' -> accessor.setChar(index, (char) value);
            case 'S' -> accessor.setShort(index, (short) value);
            case 'I' -> accessor.setInt(index, (int) value);
            case 'J' -> accessor.setLong(index, (long) value);
            case 'F' -> accessor.setFloat(index, (float) value);
            case 'D' -> accessor.setDouble(index, (double) value);
            default -> throw new RuntimeException("Should not reach here");
        }
    }

    public static void setReturnValue(EmulatedStackFrame frame, Object value) {
        if (getReturnType(frame) != void.class) {
            frame.accessor().setValue(RETURN_VALUE_IDX, value);
        }
    }

    @Nullable
    public static Object getStaticObjectField(Class<?> clazz, String fieldName) throws IllegalAccessException {
        return getDeclaredField(clazz, fieldName).get(null);
    }

    @Nullable
    public static Object getStaticObjectField(String className, String fieldName)
            throws ClassNotFoundException, IllegalAccessException {
        return getStaticObjectField(Class.forName(className), fieldName);
    }

    public static int getStaticIntField(String className, String fieldName)
            throws ClassNotFoundException, IllegalAccessException {
        return getDeclaredField(
                Class.forName(className),
                fieldName
        ).getInt(null);
    }

    public static int getIntField(Object obj, String name) throws IllegalAccessException {
        return getIntField(obj, name, null);
    }

    public static int getIntField(Object obj, String name, Class<?> clazz)
            throws IllegalAccessException {
        return getDeclaredField(
                clazz != null ? clazz : obj.getClass(),
                name
        ).getInt(obj);
    }

    public static boolean getBooleanField(Object obj, String name, Class<?> clazz)
            throws IllegalAccessException {
        return getDeclaredField(
                clazz != null ? clazz : obj.getClass(),
                name
        ).getBoolean(obj);
    }

    @Nullable
    public static Object getObjectField(Object obj, String name) throws IllegalAccessException {
        return getObjectField(obj, name, null);
    }

    @Nullable
    public static Object getObjectField(Object obj, String name, Class<?> clazz)
            throws IllegalAccessException {
        return getDeclaredField(clazz != null ? clazz : obj.getClass(), name).get(obj);
    }

    public static void setBooleanField(Object obj, String name, boolean value, Class<?> clazz)
            throws IllegalAccessException {
        final var field = getDeclaredField(clazz != null ? clazz : obj.getClass(), name);
        field.setAccessible(true);
        field.setBoolean(obj, value);
    }

    @Nullable
    public static Object callMethod(Object obj, String name, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        final var argTypes = extractArgTypes(args);
        return callMethod(obj, name, argTypes, args);
    }

    @Nullable
    public static Object callMethod(Object obj, String name, Class<?>[] argTypes, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        final var method = getDeclaredMethod(obj.getClass(), name, argTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    @Nullable
    public static Object callStaticMethod(Class<?> clazz, String name, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        final var argTypes = extractArgTypes(args);
        final var method = getDeclaredMethod(clazz, name, argTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    @Nullable
    public static Constructor<?> findConstructor(String className) throws ClassNotFoundException {
        return findConstructor(className, PARAMETER_COUNT_UNKNOWN);
    }

    @Nullable
    public static Constructor<?> findConstructor(String className, int argumentCount) throws ClassNotFoundException {
        final var clazz = Class.forName(className, true, ZygoteEntry.classLoader);

        for (var constructor : clazz.getConstructors()) {
            if (argumentCount < 0 || constructor.getParameterCount() == argumentCount) {
                return constructor;
            }
        }

        return null;
    }

    @Nullable
    public static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getField(fieldName);
            } catch (Throwable ignored) {}

            clazz = clazz.getSuperclass();
        }

        return null;
    }

    public static boolean shortyEquals(EmulatedStackFrame frame, int index, char shorty) {
        return frame.accessor().getArgumentShorty(index) == shorty;
    }

    private static Class<?>[] extractArgTypes(Object[] args) {
        final var argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i].getClass();
        }
        return argTypes;
    }
}
