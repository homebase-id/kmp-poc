package id.homebase.homebasekmppoc.prototype.lib.eventbus

import id.homebase.homebasekmppoc.prototype.lib.core.time.UnixTimeUtc
import id.homebase.homebasekmppoc.prototype.lib.drives.SharedSecretEncryptedFileHeader
import kotlin.uuid.Uuid

sealed interface  BackendEvent {
    enum class SyncSource {
        DriveSync,
        WebSocket
    }

    // A SyncUpdate event happens on a drive when either sync() has received a batch of data from
    // the host, or when the websocket listener has received some data.
    sealed interface SyncUpdate : BackendEvent {
        val driveId: Uuid  // Common property for all sync events (implement in each data class)

        data class BatchReceived(
            override val driveId : Uuid,
            val totalCount: Int,
            val batchCount: Int,
            val latestModified: UnixTimeUtc?,
            val batchData: List<SharedSecretEncryptedFileHeader>,
            val source: SyncSource = SyncSource.DriveSync
        ) : SyncUpdate

        data class SyncStarted(
            override val driveId : Uuid,
        ) : SyncUpdate // Only raised by Drive.sync()

        data class Completed(
            override val driveId: Uuid,
            val totalCount: Int
        ) : SyncUpdate  // Only raised by Drive.sync()

        data class Failed(
            override val driveId: Uuid,
            val errorMessage: String,  // Or add throwable: Throwable
            val source: SyncSource = SyncSource.DriveSync
        ) : SyncUpdate
    }

    sealed interface OutboxUpdate : BackendEvent {
        data object ProcessingStarted : OutboxUpdate

        data class Sending(
            val driveId : Uuid,
            val fileId : Uuid
        ) : OutboxUpdate  // Only raised by Drive.sync()

        data class Sent(
            val driveId : Uuid,
            val fileId : Uuid
        ) : OutboxUpdate  // Only raised by Drive.sync()

        data class Completed(
            val totalCount: Int
        ) : OutboxUpdate  // Only raised by Drive.sync()

        data class Failed(
            val errorMessage: String?  // Or add throwable: Throwable
        ) : OutboxUpdate
    }
    // Add sealed interface UploadUpdate for Outbox / upload status
    // Add sealed interface VideoUpdate (or WorkUpdate) compression & segmentation & encryption


    // We go online / offline when the websocket listener is connected / disconnected
    data object GoingOnline : BackendEvent
    data object GoingOffline : BackendEvent
}