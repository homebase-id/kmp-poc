package id.homebase.homebasekmppoc.drives

import id.homebase.homebasekmppoc.core.GuidId
import id.homebase.homebasekmppoc.crypto.EncryptedKeyHeader
import kotlinx.serialization.Serializable

/**
 * Shared secret encrypted file header
 * Ported from C# Odin.Services.Apps.SharedSecretEncryptedFileHeader
 */
@Serializable
data class SharedSecretEncryptedFileHeader(
    val fileId: GuidId,
    val targetDrive: TargetDrive,
    val fileState: FileState,
    val fileSystemType: FileSystemType,
    val sharedSecretEncryptedKeyHeader: EncryptedKeyHeader,
    val fileMetadata: ClientFileMetadata,
    val serverMetadata: ServerMetadata,
    val priority: Int = 0,
    val fileByteCount: Long = 0
) {

    fun assertFileIsActive() {
        if (fileState == FileState.Deleted) {
            throw Exception("File is deleted.")
        }
    }

    fun assertOriginalAuthor(odinId: String) {
        val originalAuthor = fileMetadata.originalAuthor
        if (originalAuthor.isNullOrEmpty()) {
            // backwards compatibility
            assertOriginalSender(odinId)
            return
        }

        if (originalAuthor != odinId) {
            throw Exception("Sender does not match original author")
        }
    }

    fun isOriginalSender(odinId: String): Boolean {
        return fileMetadata.senderOdinId == odinId
    }

    fun assertOriginalSender(odinId: String) {
        val senderOdinId = fileMetadata.senderOdinId
        if (senderOdinId.isNullOrEmpty()) {
            throw Exception(
                "Original file does not have a sender (FileId: $fileId on Drive: $targetDrive"
            )
        }

        if (!isOriginalSender(odinId)) {
            throw Exception("Sender does not match original sender")
        }
    }
}
