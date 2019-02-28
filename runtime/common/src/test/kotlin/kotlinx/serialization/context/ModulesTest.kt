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
import kotlin.test.*

class ModulesTest {
    @Serializable
    class A(val i: Int)

    @Serializable
    class B(val b: String)

    @Serializer(forClass = A::class)
    object ASerializer : KSerializer<A>

    @Serializer(forClass = B::class)
    object BSerializer : KSerializer<B>

    private fun SerialModule.assertModuleHas(aSerializer: Boolean = false, bSerializer: Boolean = false) {
        with(this) {
            assertSame(if (aSerializer) ASerializer else null, get<A>())
            assertSame(if (bSerializer) BSerializer else null, get<B>())
        }
    }


    @Test
    fun testSingletonModule() {
        val module = serializersModuleOf(A::class, ASerializer)
        module.assertModuleHas(
            aSerializer = true,
            bSerializer = false
        )
    }

    @Test
    fun testMapModule() {
        val module1 = serializersModuleOf(mapOf(B::class to BSerializer))
        module1.assertModuleHas(
            aSerializer = false,
            bSerializer = true
        )

        serializersModuleOf(mapOf(A::class to ASerializer, B::class to BSerializer)).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )

        (module1 + serializersModuleOf(A::class, ASerializer)).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )
    }

    @Test
    fun testCompositeModule() {
        val moduleA = serializersModule(ASerializer)
        val moduleB = serializersModuleOf(mapOf(B::class to BSerializer))

        (moduleA + moduleB).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )

        var composite: SerialModule = CompositeModule()
        composite.assertModuleHas(
            aSerializer = false,
            bSerializer = false
        )
        composite += moduleA
        composite.assertModuleHas(
            aSerializer = true,
            bSerializer = false
        )
        composite += moduleB
        composite.assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )
    }

    @Test
    fun testOverwriteSerializer() {
        val moduleA = SerializersModule {
            contextual(A::class, ASerializer)
            assertFailsWith<SerializerAlreadyRegisteredException> {
                contextual(A::class, ASerializer)
            }
        }
        moduleA.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testPlusIsLeftBiased() {
        val incorrect = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to BSerializer))
        val correct = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to ASerializer))
        correct.assertModuleHas(aSerializer = true, bSerializer = false)
        val sum = correct + incorrect
        sum.assertModuleHas(aSerializer = true, bSerializer = false)
    }
}
