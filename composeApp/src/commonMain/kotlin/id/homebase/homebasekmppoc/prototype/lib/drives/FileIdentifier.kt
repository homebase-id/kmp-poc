@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Identifies a file using one of three methods: FileId, GlobalTransitId, or UniqueId.
 * Only one identifier field can be set at a time.
 */
data class FileIdentifier(
    val fileId: Uuid? = null,
    val globalTransitId: Uuid? = null,
    val uniqueId: Uuid? = null,
    val targetDrive: TargetDrive
) {
    private val fileIdHasValue: Boolean
        get() = fileId != null && fileId != Uuid.NIL

    private val globalTransitIdHasValue: Boolean
        get() = globalTransitId != null && globalTransitId != Uuid.NIL

    private val uniqueIdHasValue: Boolean
        get() = uniqueId != null && uniqueId != Uuid.NIL

    /**
     * Validates that the file identifier is properly configured.
     * Throws OdinClientException if invalid.
     */
    fun assertIsValid() {
        val missingField = !fileIdHasValue &&
                !globalTransitIdHasValue &&
                !uniqueIdHasValue

        if (missingField || !targetDrive.isValid()) {
            throw OdinClientException("The file identifier is invalid")
        }

        if (fileIdHasValue && (globalTransitIdHasValue || uniqueIdHasValue)) {
            throw OdinClientException("The file identifier is invalid; only one field can be set")
        }

        if (globalTransitIdHasValue && (fileIdHasValue || uniqueIdHasValue)) {
            throw OdinClientException("The file identifier is invalid; only one field can be set")
        }

        if (uniqueIdHasValue && (globalTransitIdHasValue || fileIdHasValue)) {
            throw OdinClientException("The file identifier is invalid; only one field can be set")
        }
    }

    /**
     * Validates that the file identifier is valid and of the expected type.
     */
    fun assertIsValid(expectedType: FileIdentifierType) {
        assertIsValid()
        assertIsType(expectedType)
    }

    /**
     * Returns the type of identifier being used.
     */
    fun getFileIdentifierType(): FileIdentifierType {
        assertIsValid()

        return when {
            fileIdHasValue -> FileIdentifierType.File
            globalTransitIdHasValue -> FileIdentifierType.GlobalTransitId
            uniqueIdHasValue -> FileIdentifierType.UniqueId
            else -> FileIdentifierType.NotSet
        }
    }

    /**
     * Converts this FileIdentifier to a GlobalTransitIdFileIdentifier.
     */
    fun toGlobalTransitIdFileIdentifier(): GlobalTransitIdFileIdentifier {
        return GlobalTransitIdFileIdentifier(
            targetDrive = targetDrive,
            globalTransitId = globalTransitId ?: Uuid.NIL
        )
    }

    private fun assertIsType(expectedType: FileIdentifierType) {
        when (expectedType) {
            FileIdentifierType.File -> {
                if (!fileIdHasValue) {
                    throw OdinClientException("The file identifier type is invalid")
                }
            }
            FileIdentifierType.GlobalTransitId -> {
                if (!globalTransitIdHasValue) {
                    throw OdinClientException("The file identifier type is invalid")
                }
            }
            FileIdentifierType.UniqueId -> {
                if (!uniqueIdHasValue) {
                    throw OdinClientException("The file identifier type is invalid")
                }
            }
            FileIdentifierType.NotSet -> {
                throw IllegalArgumentException("Cannot assert NotSet type")
            }
        }
    }
}

/**
 * Types of file identifiers supported by the Odin system.
 */
enum class FileIdentifierType {
    NotSet,
    File,
    GlobalTransitId,
    UniqueId
}

/**
 * Exception thrown when a file identifier operation fails.
 */
class OdinClientException(message: String) : Exception(message)
