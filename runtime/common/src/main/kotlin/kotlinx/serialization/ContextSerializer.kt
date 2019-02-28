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

package kotlinx.serialization

import kotlinx.serialization.module.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KClass

/**
 * This class provides support for retrieving a serializer in runtime,
 * instead of using the one precompiled by the serialization plugin.
 *
 * Typical usage of ContextSerializer would be serialization of class which does not have
 * precompiled serializer (e.g. Java class or class from 3rd party library);
 * or desire to override serialized class form in one dedicated output format.
 *
 * Serializers are being looked for in [SerialModule] which tied to particular [Encoder] or [Decoder],
 * using statically-known (compile-time) [KClass].
 * To create serial module, use [SerializersModule] constructor function.
 * To pass it to encoder and decoder, consult particular [SerialFormat]'s documentation.
 */
@ImplicitReflectionSerializer
class ContextSerializer<T : Any>(private val serializableClass: KClass<T>) : KSerializer<T> {
    override fun serialize(encoder: Encoder, obj: T) {
        val s = encoder.context.getByValueOrDefault(obj)
        encoder.encodeSerializableValue(s, obj)
    }

    override fun deserialize(decoder: Decoder): T {
        val s = decoder.context.getOrDefault(serializableClass)
        @Suppress("UNCHECKED_CAST")
        return decoder.decodeSerializableValue(s)
    }

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("CONTEXT") {} // todo: remove this crutch
}
