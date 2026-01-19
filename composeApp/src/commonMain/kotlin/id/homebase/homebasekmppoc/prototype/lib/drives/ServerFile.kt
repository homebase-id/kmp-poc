package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.core.SecureByteArray
import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.crypto.KeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.files.FileMetadata
import id.homebase.homebasekmppoc.prototype.lib.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

/**
 * The file data as it is sent back from the server.  Private provider use only
 */
@Serializable
data class ServerFile(
    @Serializable(with = UuidSerializer::class)
    val fileId: Uuid,
    val driveId: Uuid,
    val fileState: FileState,
    val fileSystemType: FileSystemType,
    val sharedSecretEncryptedKeyHeader: EncryptedKeyHeader,
    val fileMetadata: FileMetadata,
    val serverMetadata: ServerMetadata,
    val priority: Int = 0,
    val fileByteCount: Long = 0
) {
    suspend fun asHomebaseFile(sharedSecret: SecureByteArray): HomebaseFile {
        val resolvedKeyHeader: KeyHeader
        val resolvedMetadata: FileMetadata

        if (fileMetadata.isEncrypted) {
            if (sharedSecretEncryptedKeyHeader == EncryptedKeyHeader.empty()) {
                throw FileDecryptionException.MissingEncryptedHeader()
            }

            resolvedKeyHeader = try {
                sharedSecretEncryptedKeyHeader.decryptAesToKeyHeader(sharedSecret)
            } catch (e: Throwable) {
                throw FileDecryptionException.KeyHeaderDecryptionFailed(e)
            }

            val content = fileMetadata.appData.content

            resolvedMetadata =
                if (content == null || content.isEmpty()) {
                    // Encrypted but empty content is valid
                    fileMetadata.withDecryptedContent(ByteArray(0))
                } else {
                    val encryptedBytes = try {
                        Base64.decode(content)
                    } catch (e: Throwable) {
                        throw FileDecryptionException.ContentBase64DecodeFailed(e)
                    }

                    val decryptedBytes = try {
                        resolvedKeyHeader.decrypt(encryptedBytes)
                    } catch (e: Throwable) {
                        throw FileDecryptionException.ContentDecryptionFailed(e)
                    }

                    fileMetadata.withDecryptedContent(decryptedBytes)
                }
        } else {
            resolvedKeyHeader = KeyHeader.empty()
            resolvedMetadata = fileMetadata
        }

        return HomebaseFile(
            fileId = fileId,
            driveId = driveId,
            fileState = fileState,
            fileSystemType = fileSystemType,
            keyHeader = resolvedKeyHeader,
            fileMetadata = resolvedMetadata,
            serverMetadata = serverMetadata,
            priority = priority,
            fileByteCount = fileByteCount
        )
    }


}

fun FileMetadata.withDecryptedContent(bytes: ByteArray): FileMetadata =
    copy(
        appData = appData.copy(
            content = bytes.decodeToString()
        ),
        isEncrypted = false
    )

sealed class FileDecryptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class MissingEncryptedHeader :
        FileDecryptionException("File is marked encrypted but has no encrypted key header")

    class KeyHeaderDecryptionFailed(cause: Throwable) :
        FileDecryptionException("Failed to decrypt key header", cause)

    class ContentBase64DecodeFailed(cause: Throwable) :
        FileDecryptionException("Failed to decode encrypted content (Base64)", cause)

    class ContentDecryptionFailed(cause: Throwable) :
        FileDecryptionException("Failed to decrypt file content", cause)
}
