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

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * A [SerialModule] for composing other modules
 *
 * @see SerialModule.plus
 */
internal class CompositeModule(val modules: List<SerialModule> = listOf()): SerialModule {

    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? =
        findInModules { get(kclass) }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, obj: T): KSerializer<out T>? =
        findInModules { resolveFromBase(basePolyType, obj) }

    override fun <T : Any> resolveFromBase(basePolyType: KClass<T>, serializedClassName: String): KSerializer<out T>? =
        findInModules { resolveFromBase(basePolyType, serializedClassName) }

    private inline fun <R> findInModules(maybeResult: SerialModule.() -> R?): R? {
        modules.forEach { module ->
            module.maybeResult()?.let { return it }
        }
        return null
    }
}

/**
 * Composes [this] module with [other].
 *
 * Module from the left-hand side have higher priority, i.e.
 * if KClass `A` is registered in both modules,
 * serializer from the left module would be taken.
 */
@Suppress("RedundantVisibilityModifier")
public operator fun SerialModule.plus(other: SerialModule): SerialModule {
    val list1 = if (this is CompositeModule) this.modules else listOf(this)
    val list2 = if (other is CompositeModule) other.modules else listOf(other)
    return CompositeModule(list1 + list2)
}
