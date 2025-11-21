package id.homebase.homebasekmppoc.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Encryption type enumeration
 */
@Serializable
enum class EncryptionType {
    @SerialName("aes")
    Aes
}
