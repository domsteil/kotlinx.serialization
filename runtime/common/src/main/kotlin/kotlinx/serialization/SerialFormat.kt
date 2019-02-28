package kotlinx.serialization

import kotlinx.serialization.module.SerialModule
import kotlinx.serialization.module.plus
import kotlinx.serialization.internal.HexConverter

internal const val InstallDeprecationText = "Install mutates format instance, pass module in constructor"

interface SerialFormat {
    @Deprecated(InstallDeprecationText)
    fun install(module: SerialModule)

    val context: SerialModule
}

abstract class AbstractSerialFormat(context: SerialModule): SerialFormat {

    // this could be a constructor val after `install` would be deleted
    final override var context: SerialModule = context
        private set

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(InstallDeprecationText)
    override fun install(module: SerialModule) {
        context = module + context
    }
}

interface BinaryFormat: SerialFormat {
    fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray
    fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T
}

fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, obj: T): String =
    HexConverter.printHexBinary(dump(serializer, obj), lowerCase = true)

fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T =
    load(deserializer, HexConverter.parseHexBinary(hex))

interface StringFormat: SerialFormat {
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
    fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T
}
