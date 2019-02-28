@file:Suppress("UNCHECKED_CAST", "RedundantVisibilityModifier")

package kotlinx.serialization.module

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * Serial module is a collection of serializers used by [ContextSerializer] and [PolymorphicSerializer]
 * to override or provide serializers at the runtime, whereas at he compile-time they provided by the
 * serialization plugin.
 *
 * It can be regarded as a map where serializers are found using statically known KClasses.
 *
 * To allow a runtime resolving of serializers and usage of the particular SerialModule,
 * one of the special annotations must be used.
 *
 * @see ContextualSerialization
 * @see Polymorphic
 */
public interface SerialModule {

    /**
     * Returns a dependent serializer associated with a given [kclass].
     *
     * This method is used in context-sensitive operations
     * on a property marked with [ContextualSerialization], by a [ContextSerializer]
     */
    public operator fun <T: Any> get(kclass: KClass<T>): KSerializer<T>?

    /**
     * Returns serializer registered for polymorphic serialization of an [obj]'s class in a scope of [basePolyType].
     *
     * This method is used inside a [PolymorphicSerializer] when statically known class of a property marked with [Polymorphic]
     * is [basePolyType], and the actual object in this property is [obj].
     */
    public fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>?

    /**
     * Returns serializer registered for polymorphic serialization of a class with [serializedClassName] in a scope of [basePolyType].
     *
     * This method is used inside a [PolymorphicSerializer] when statically known class of a property marked with [Polymorphic]
     * is [basePolyType], and the class name received from [Decoder] is a [serializedClassName].
     */
    public fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>?
}

/**
 * Returns a dependent serializer associated with a given reified type.
 *
 * This method is used in context-sensitive operations
 * on a property marked with [ContextualSerialization], by a [ContextSerializer]
 */
public inline fun <reified T: Any> SerialModule.get(): KSerializer<T>? = get(T::class)

/**
 * Returns a serializer associated with KClass which given [value] has.
 *
 * This method is used in context-sensitive operations
 * on a property marked with [ContextualSerialization], by a [ContextSerializer]
 */
public fun <T: Any> SerialModule.getByValue(value: T): KSerializer<T>? {
    val klass = value::class
    return get(klass) as? KSerializer<T>
}

@ImplicitReflectionSerializer
public fun <T: Any> SerialModule?.getOrDefault(klass: KClass<T>) = this?.let { get(klass) } ?: klass.serializer()

@ImplicitReflectionSerializer
public fun <T: Any> SerialModule?.getByValueOrDefault(value: T): KSerializer<T> = this?.let { getByValue(value) } ?: value::class.serializer() as KSerializer<T>

/**
 * A [SerialModule] which always returns `null`.
 */
public object EmptyModule: SerialModule {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? = null
    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? =
        null

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        null
}
