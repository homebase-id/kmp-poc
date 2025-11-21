package id.homebase.homebasekmppoc.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Custom serializer for ByteArray that uses Base64 encoding
 * Matches C# behavior where byte[] is automatically serialized to/from Base64 strings
 */
@OptIn(ExperimentalEncodingApi::class)
object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64String = Base64.encode(value)
        encoder.encodeString(base64String)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64String = decoder.decodeString()
        return Base64.decode(base64String)
    }
}
