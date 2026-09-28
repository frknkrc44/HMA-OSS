package org.frknkrc44.hma_oss.xposed.util

import org.frknkrc44.hma_oss.xposed.service.HookFrame
import org.frknkrc44.hma_oss.xposed.service.SystemServerHook
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Backend-local implementation of the helpers used by the upstream business hooks. */
object ZLUtils {
    val HookFrame.returnType: Class<*> get() = resultType
    val HookFrame.thisObject: Any get() = requireNotNull(receiver) { "Instance receiver is null: $executable" }
    val HookFrame.args: Array<Any?> get() = values
    val HookFrame.argTypes: Array<Class<*>> get() = types
    // Preserve upstream's JVM behavior: a reference argument can be null even
    // though its Kotlin signature is Any. Array<Any?> and Array<Any> have the
    // same runtime representation; reading an element adds no null assertion.
    @Suppress("UNCHECKED_CAST")
    fun HookFrame.getArgument(index: Int): Any = (values as Array<Any>)[index]
    fun HookFrame.getArgumentType(index: Int): Class<*> = types[index]
    fun HookFrame.setArgument(index: Int, value: Any?) { values[index] = value }
    fun HookFrame.shortyEquals(index: Int, shorty: Char): Boolean = when (types[index]) {
        Boolean::class.javaPrimitiveType -> shorty == 'Z'
        Byte::class.javaPrimitiveType -> shorty == 'B'
        Char::class.javaPrimitiveType -> shorty == 'C'
        Short::class.javaPrimitiveType -> shorty == 'S'
        Int::class.javaPrimitiveType -> shorty == 'I'
        Long::class.javaPrimitiveType -> shorty == 'J'
        Float::class.javaPrimitiveType -> shorty == 'F'
        Double::class.javaPrimitiveType -> shorty == 'D'
        else -> shorty == 'L'
    }

    private fun load(name: String) = Class.forName(name, false, SystemServerHook.classLoader)
    private fun field(clazz: Class<*>, name: String): Field =
        findField(clazz, name) ?: throw NoSuchFieldException("${clazz.name}#$name")

    fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    fun getStaticObjectField(className: String, name: String): Any? = field(load(className), name).get(null)
    fun getStaticIntField(className: String, name: String): Int = field(load(className), name).getInt(null)
    fun getIntField(obj: Any, name: String, clazz: Class<*>? = null): Int = field(clazz ?: obj.javaClass, name).getInt(obj)
    fun getBooleanField(obj: Any, name: String, clazz: Class<*>? = null): Boolean = field(clazz ?: obj.javaClass, name).getBoolean(obj)
    fun getObjectField(obj: Any, name: String, clazz: Class<*>? = null): Any? = field(clazz ?: obj.javaClass, name).get(obj)
    fun setBooleanField(obj: Any, name: String, value: Boolean, clazz: Class<*>? = null) = field(clazz ?: obj.javaClass, name).setBoolean(obj, value)

    fun callMethodWithTypes(obj: Any, name: String, types: Array<Class<*>>, args: Array<Any>): Any? {
        val method = methods(obj.javaClass).firstOrNull { it.name == name && it.parameterTypes.contentEquals(types) }
            ?: throw NoSuchMethodException("${obj.javaClass.name}#$name")
        // Preserve ordinary reflective-call semantics (the full chain). The PM
        // lookup helpers may transitively enter HMA's own package-filter hooks;
        // selecting ORIGIN here would silently bypass those hooks.
        return SystemServerHook.framework.getInvoker(method).invoke(obj, *args)
    }

    fun callMethod(obj: Any, name: String, vararg args: Any?): Any? =
        invoke(obj.javaClass, obj, name, args)

    fun callStaticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? =
        invoke(clazz, null, name, args)

    private fun invoke(clazz: Class<*>, receiver: Any?, name: String, args: Array<out Any?>): Any? {
        val candidates = methods(clazz).filter {
            it.name == name && it.parameterCount == args.size &&
                Modifier.isStatic(it.modifiers) == (receiver == null) &&
                it.parameterTypes.zip(args).all { (type, arg) ->
                    if (arg == null) !type.isPrimitive else boxed(type).isInstance(arg)
                }
        }.toList()
        val method = candidates.firstOrNull { candidate ->
            candidate.parameterTypes.zip(args).all { (type, arg) -> arg != null && boxed(type) == arg.javaClass }
        } ?: candidates.firstOrNull() ?: throw NoSuchMethodException("${clazz.name}#$name")
        return SystemServerHook.framework.getInvoker(method).invoke(receiver, *args)
    }

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        Boolean::class.javaPrimitiveType -> java.lang.Boolean::class.java
        Byte::class.javaPrimitiveType -> java.lang.Byte::class.java
        Char::class.javaPrimitiveType -> java.lang.Character::class.java
        Short::class.javaPrimitiveType -> java.lang.Short::class.java
        Int::class.javaPrimitiveType -> java.lang.Integer::class.java
        Long::class.javaPrimitiveType -> java.lang.Long::class.java
        Float::class.javaPrimitiveType -> java.lang.Float::class.java
        Double::class.javaPrimitiveType -> java.lang.Double::class.java
        else -> type
    }

    private fun methods(clazz: Class<*>): Sequence<Method> =
        generateSequence(clazz) { it.superclass }.flatMap { it.declaredMethods.asSequence() }

    fun findConstructor(className: String, argumentCount: Int = -1): Constructor<*>? =
        load(className).declaredConstructors.firstOrNull { argumentCount < 0 || it.parameterCount == argumentCount }
            ?.apply { isAccessible = true }

    fun findMethod(className: String, name: String, isDeclared: Boolean = false,
        systemClassLoader: Boolean = false, vararg args: Class<*>): Method {
        val clazz = load(className)
        return (if (isDeclared) clazz.getDeclaredMethod(name, *args) else clazz.getMethod(name, *args))
            .apply { isAccessible = true }
    }
}
