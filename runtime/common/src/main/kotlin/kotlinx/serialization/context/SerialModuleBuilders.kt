@file:Suppress("RedundantVisibilityModifier")
/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.context

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * A builder inside DSL for building [SerialModule]
 *
 * Instance of this builder could be obtained via [SerializersModule] function
 */
public class SerializersModuleBuilder
@PublishedApi internal constructor(@PublishedApi internal val impl: SerialModuleImpl) {

    /**
     * Add to this module [serializer] which will be used whe [kClass] is
     * needed to be serialized contextually.
     *
     * If serializer already registered for the given KClass, an exception is thrown.
     * To override registered serializers, combine built module with another using
     * left-biased [SerialModule.plus]
     *
     * @see ContextSerializer
     */
    public fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) =
        impl.registerSerializer(kClass, serializer)

    /**
     * Creates a builder to register all subclasses of a given [baseClass]
     * for polymorphic serialization. If [baseSerializer] is not null, registers it as a serializer for [baseClass]
     * (which is useful if base class is serializable). To add subclasses, use
     * [PolymorphicModuleBuilder.addSubclass] or [PolymorphicModuleBuilder.unaryPlus].
     *
     * If serializer already registered for the given KClass in the given scope, an exception is thrown.
     * To override registered serializers, combine built module with another using
     * left-biased [SerialModule.plus]
     *
     * @see PolymorphicSerializer
     */
    public inline fun <Base : Any> polymorphic(
        baseClass: KClass<Base>,
        baseSerializer: KSerializer<Base>? = null,
        build: PolymorphicModuleBuilder<Base>.() -> Unit = {}
    ) {
        val builder = PolymorphicModuleBuilder(baseClass, baseSerializer)
        builder.build()
        builder.buildTo(impl)
    }

    /**
     * Creates a builder to register all serializable subclasses for polymorphic serialization
     * for multiple base classes. This is useful when you have more two or more super classes in a big hierarchy, e.g.:
     *
     * ```
     * interface I
     * @Serializable abstract class A() : I
     * @Serializable final class B : A()
     * @Serializable class Message(@Polymorphic val i: I, @Polymorphic val a: A)
     * ```
     *
     * In this case, you have to register B as subclass for two base classes: I and A.
     *
     * If serializer already registered for the given KClass in the given scope, an exception is thrown.
     * To override registered serializers, combine built module with another using
     * left-biased [SerialModule.plus]
     *
     * @see PolymorphicSerializer
     */
    @Suppress("UNCHECKED_CAST")
    public inline fun polymorphic(
        vararg bases: Pair<KClass<*>, KSerializer<*>?>,
        build: PolymorphicModuleBuilder<*>.() -> Unit = {}
    ) {
        if (bases.isEmpty()) return
        val (firstBase, firstSerializer) = bases[0]
        val builder = PolymorphicModuleBuilder(firstBase as KClass<Any>, firstSerializer as KSerializer<Any>)
        builder.build()
        builder.buildTo(impl)
        for ((base, serial) in bases.drop(1)) {
            builder.changeBase(base as KClass<Any>, serial as KSerializer<Any>).buildTo(impl)
        }
    }
}

/**
 * A DSL for creating a [SerialModule].
 *
 * Serializers can be add via [SerializersModuleBuilder.contextual] or [SerializersModuleBuilder.polymorphic]
 */
@Suppress("FunctionName")
public inline fun SerializersModule(build: SerializersModuleBuilder.() -> Unit): SerialModule {
    val builder = SerializersModuleBuilder(SerialModuleImpl())
    builder.build()
    return builder.impl
}


@Deprecated(deprecationText, ReplaceWith("serializersModuleOf"))
typealias SimpleModule<@Suppress("UNUSED_TYPEALIAS_PARAMETER") T> = SerialModule

/**
 * Returns a [SerialModule] which has one class with one serializer for [ContextSerializer].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Any> serializersModuleOf(kClass: KClass<T>, serializer: KSerializer<T>): SerialModule =
    SerializersModule { contextual(kClass, serializer) }

/**
 * Shortcut for [serializersModuleOf] function with type parameter.
 */
// it could be named `serializersModuleOf`, too, but https://youtrack.jetbrains.com/issue/KT-30176.
public inline fun <reified T : Any> serializersModule(serializer: KSerializer<T>): SerialModule =
    serializersModuleOf(T::class, serializer)


/**
 * Returns a [SerialModule] which has multiple classes with its serializers for [ContextSerializer].
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
public inline fun serializersModuleOf(map: Map<KClass<*>, KSerializer<*>>): SerialModule = SerializersModule {
    map.forEach { (k, s) -> contextual(k as KClass<Any>, s as KSerializer<Any>) }
}
