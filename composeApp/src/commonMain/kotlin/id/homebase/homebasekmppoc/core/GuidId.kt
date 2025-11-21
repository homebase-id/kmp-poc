@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * GuidId: Represents a GUID (Globally Unique Identifier)
 * Ported from C# Odin.Core.GuidId (which wraps System.Guid)
 */
object GuidIdSerializer : KSerializer<GuidId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GuidId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GuidId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): GuidId {
        return GuidId(decoder.decodeString())
    }
}

@Serializable(with = GuidIdSerializer::class)
data class GuidId(val value: String) {

    init {
        require(isValid(value)) { "Invalid GUID format: $value" }
    }

    fun clone(): GuidId {
        return GuidId(value)
    }

    override fun toString(): String {
        return value
    }

    companion object {
        val Empty = GuidId("00000000-0000-0000-0000-000000000000")

        fun isValid(guid: String?): Boolean {
            if (guid.isNullOrEmpty()) return false
            try {
                Uuid.parse(guid)
                return true
            } catch (e: Exception) {
                return false
            }
        }

        fun isValid(guid: GuidId?): Boolean {
            return guid != null && isValid(guid.value)
        }

        @OptIn(ExperimentalUuidApi::class)
        fun newGuid(): GuidId {
            return GuidId(Uuid.random().toString())
        }
    }
}

// Platform-specific expect function for GUID generation
