@file:OptIn(ExperimentalUuidApi::class)

package id.homebase.homebasekmppoc.prototype.lib.drives

import id.homebase.homebasekmppoc.lib.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base class for payload retrieval requests.
 * Defines common properties for chunk-based file retrieval with optional encryption key.
 *
 * Ported from C# Odin.Services.Drives.GetPayloadRequestBase
 */
@Serializable
abstract class GetPayloadRequestBase {
    /**
     * Optional chunk specification for range-based file retrieval.
     * If null, the entire file payload is retrieved.
     */
    var chunk: FileChunk? = null

    /**
     * Optional encryption key for decrypting the payload.
     */
    var key: String? = null
}

/**
 * Request to retrieve a file payload by external file identifier.
 * Used for standard file retrieval with drive alias and file ID.
 *
 * Ported from C# Odin.Services.Drives.GetPayloadRequest
 */
@Serializable
data class GetPayloadRequest(
    /**
     * The external file identifier specifying target drive and file ID.
     */
    var file: ExternalFileIdentifier? = null
) : GetPayloadRequestBase()

/**
 * Request to retrieve a file payload by global transit ID.
 * Used for retrieving files using their global transit identifier.
 *
 * Ported from C# Odin.Services.Drives.GetPayloadByGlobalTransitIdRequest
 */
@Serializable
data class GetPayloadByGlobalTransitIdRequest(
    /**
     * The global transit ID file identifier.
     */
    var file: GlobalTransitIdFileIdentifier? = null
) : GetPayloadRequestBase()

/**
 * Request to retrieve a file payload by unique ID.
 * Used for retrieving files using their unique identifier within a specific drive.
 *
 * Ported from C# Odin.Services.Drives.GetPayloadByUniqueIdRequest
 */
@Serializable
data class GetPayloadByUniqueIdRequest(
    /**
     * The unique ID of the file to retrieve.
     */
    @Serializable(with = UuidSerializer::class)
    var uniqueId: Uuid? = null,

    /**
     * The target drive containing the file.
     */
    var targetDrive: TargetDrive? = null
) : GetPayloadRequestBase()

/**
 * Request to retrieve a file header by unique ID.
 * Used for retrieving file metadata without the payload.
 *
 * Ported from C# Odin.Services.Drives.GetFileHeaderByUniqueIdRequest
 */
@Serializable
data class GetFileHeaderByUniqueIdRequest(
    /**
     * The unique ID of the file.
     */
    @Serializable(with = UuidSerializer::class)
    var uniqueId: Uuid? = null,

    /**
     * The target drive containing the file.
     */
    var targetDrive: TargetDrive? = null
)
