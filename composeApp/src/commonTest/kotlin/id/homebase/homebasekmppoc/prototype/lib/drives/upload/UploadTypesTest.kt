package id.homebase.homebasekmppoc.prototype.lib.drives.upload

import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

/** Unit tests for Upload Types */
class UploadTypesTest {

    // Test data
    private val testTargetDrive =
            TargetDrive(
                    alias = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                    type = Uuid.parse("00000000-0000-0000-0000-000000000002")
            )

    // ========== SendContents Tests ==========

    @Test
    fun `SendContents values are correct`() {
        assertEquals(0, SendContents.HeaderOnly.value)
        assertEquals(1, SendContents.Thumbnails.value)
        assertEquals(2, SendContents.Payload.value)
        assertEquals(3, SendContents.All.value)
    }

    @Test
    fun `SendContents fromInt works correctly`() {
        assertEquals(SendContents.HeaderOnly, SendContents.fromInt(0))
        assertEquals(SendContents.Thumbnails, SendContents.fromInt(1))
        assertEquals(SendContents.Payload, SendContents.fromInt(2))
        assertEquals(SendContents.All, SendContents.fromInt(3))
        assertEquals(
                SendContents.HeaderOnly,
                SendContents.fromInt(999)
        ) // Unknown defaults to HeaderOnly
    }

    // ========== ScheduleOptions Tests ==========

    @Test
    fun `ScheduleOptions values are correct`() {
        assertEquals("sendNowAwaitResponse", ScheduleOptions.SendNowAwaitResponse.value)
        assertEquals("sendAsync", ScheduleOptions.SendLater.value)
    }

    @Test
    fun `ScheduleOptions fromString works correctly`() {
        assertEquals(
                ScheduleOptions.SendNowAwaitResponse,
                ScheduleOptions.fromString("sendNowAwaitResponse")
        )
        assertEquals(ScheduleOptions.SendLater, ScheduleOptions.fromString("sendAsync"))
        assertEquals(
                ScheduleOptions.SendLater,
                ScheduleOptions.fromString("unknown")
        ) // Unknown defaults to SendLater
    }

    // ========== PriorityOptions Tests ==========

    @Test
    fun `PriorityOptions values are correct`() {
        assertEquals(1, PriorityOptions.High.value)
        assertEquals(2, PriorityOptions.Medium.value)
        assertEquals(3, PriorityOptions.Low.value)
    }

    @Test
    fun `PriorityOptions fromInt works correctly`() {
        assertEquals(PriorityOptions.High, PriorityOptions.fromInt(1))
        assertEquals(PriorityOptions.Medium, PriorityOptions.fromInt(2))
        assertEquals(PriorityOptions.Low, PriorityOptions.fromInt(3))
        assertEquals(
                PriorityOptions.Medium,
                PriorityOptions.fromInt(999)
        ) // Unknown defaults to Medium
    }

    // ========== TransferUploadStatus Tests ==========

    @Test
    fun `TransferUploadStatus values are correct`() {
        assertEquals("enqueued", TransferUploadStatus.Enqueued.value)
        assertEquals("enqueuedfailed", TransferUploadStatus.EnqueuedFailed.value)
        assertEquals("deliveredtoinbox", TransferUploadStatus.DeliveredToInbox.value)
    }

    @Test
    fun `TransferUploadStatus fromString works correctly with case insensitive matching`() {
        assertEquals(TransferUploadStatus.Enqueued, TransferUploadStatus.fromString("enqueued"))
        assertEquals(TransferUploadStatus.Enqueued, TransferUploadStatus.fromString("ENQUEUED"))
        assertEquals(
                TransferUploadStatus.DeliveredToInbox,
                TransferUploadStatus.fromString("deliveredtoinbox")
        )
        assertEquals(
                TransferUploadStatus.Enqueued,
                TransferUploadStatus.fromString("unknown")
        ) // Unknown defaults to Enqueued
    }

    // ========== StorageOptions Tests ==========

    @Test
    fun `StorageOptions creation works correctly`() {
        val options =
                StorageOptions(
                        drive = testTargetDrive,
                        expiresTimestamp = 1702656000L,
                        storageIntent = "metadataOnly"
                )

        assertEquals(testTargetDrive, options.drive)
        assertEquals(1702656000L, options.expiresTimestamp)
        assertEquals("metadataOnly", options.storageIntent)
    }

    // ========== PushNotificationOptions Tests ==========

    @Test
    fun `PushNotificationOptions creation works correctly`() {
        val options =
                PushNotificationOptions(
                        appId = "test-app",
                        typeId = "test-type",
                        tagId = "test-tag",
                        silent = false,
                        unEncryptedMessage = "Hello World",
                        recipients = listOf("user1", "user2")
                )

        assertEquals("test-app", options.appId)
        assertEquals("test-type", options.typeId)
        assertEquals("test-tag", options.tagId)
        assertEquals(false, options.silent)
        assertEquals("Hello World", options.unEncryptedMessage)
        assertEquals(listOf("user1", "user2"), options.recipients)
    }

    // ========== TransitOptions Factory Tests ==========

    @Test
    fun `TransitOptions withoutNotifications creates correct options`() {
        val options =
                TransitOptions.withoutNotifications(
                        recipients = listOf("user1"),
                        schedule = ScheduleOptions.SendNowAwaitResponse,
                        priority = PriorityOptions.High,
                        sendContents = SendContents.All
                )

        assertEquals(listOf("user1"), options.recipients)
        assertEquals(ScheduleOptions.SendNowAwaitResponse, options.schedule)
        assertEquals(PriorityOptions.High, options.priority)
        assertEquals(SendContents.All, options.sendContents)
        assertEquals(false, options.useAppNotification)
        assertEquals(null, options.appNotificationOptions)
    }

    @Test
    fun `TransitOptions withNotifications creates correct options`() {
        val notificationOptions =
                PushNotificationOptions(
                        appId = "app",
                        typeId = "type",
                        tagId = "tag",
                        silent = true
                )

        val options =
                TransitOptions.withNotifications(
                        recipients = listOf("user1"),
                        schedule = ScheduleOptions.SendLater,
                        priority = PriorityOptions.Low,
                        sendContents = SendContents.Thumbnails,
                        appNotificationOptions = notificationOptions
                )

        assertEquals(true, options.useAppNotification)
        assertEquals(notificationOptions, options.appNotificationOptions)
    }

    @Test
    fun `TransitOptions onlyNotifications creates correct options`() {
        val notificationOptions =
                PushNotificationOptions(
                        appId = "app",
                        typeId = "type",
                        tagId = "tag",
                        silent = false
                )

        val options = TransitOptions.onlyNotifications(notificationOptions)

        assertEquals(true, options.useAppNotification)
        assertEquals(notificationOptions, options.appNotificationOptions)
        assertEquals(null, options.recipients)
        assertEquals(null, options.schedule)
    }

    // ========== UploadFileDescriptor Tests ==========

    @Test
    fun `UploadAppFileMetaData creation works correctly`() {
        val metadata =
                UploadAppFileMetaData(
                        uniqueId = "unique-123",
                        tags = listOf("tag1", "tag2"),
                        fileType = 100,
                        dataType = 200,
                        content = "Test content"
                )

        assertEquals("unique-123", metadata.uniqueId)
        assertEquals(listOf("tag1", "tag2"), metadata.tags)
        assertEquals(100, metadata.fileType)
        assertEquals(200, metadata.dataType)
        assertEquals("Test content", metadata.content)
    }

    @Test
    fun `UploadFileMetadata creation works correctly`() {
        val appMetadata = UploadAppFileMetaData(uniqueId = "test")
        val metadata =
                UploadFileMetadata(
                        allowDistribution = true,
                        isEncrypted = false,
                        appData = appMetadata,
                        versionTag = "v1"
                )

        assertEquals(true, metadata.allowDistribution)
        assertEquals(false, metadata.isEncrypted)
        assertEquals(appMetadata, metadata.appData)
        assertEquals("v1", metadata.versionTag)
    }

    // ========== UploadManifest Tests ==========

    @Test
    fun `UploadPayloadDescriptor creation works correctly`() {
        val descriptor =
                UploadPayloadDescriptor(
                        payloadKey = "payload1",
                        descriptorContent = "content",
                        contentType = "application/json"
                )

        assertEquals("payload1", descriptor.payloadKey)
        assertEquals("content", descriptor.descriptorContent)
        assertEquals("application/json", descriptor.contentType)
    }

    @Test
    fun `UploadManifest creation works correctly`() {
        val descriptors =
                listOf(
                        UploadPayloadDescriptor(payloadKey = "payload1"),
                        UploadPayloadDescriptor(payloadKey = "payload2")
                )
        val manifest = UploadManifest(payloadDescriptors = descriptors)

        assertEquals(2, manifest.payloadDescriptors?.size)
        assertEquals("payload1", manifest.payloadDescriptors?.get(0)?.payloadKey)
    }

    @Test
    fun `UpdatePayloadInstruction creation works correctly`() {
        val instruction =
                UpdatePayloadInstruction(
                        payloadKey = "payload1",
                        operationType = PayloadOperationType.AppendOrOverwrite,
                        contentType = "text/plain"
                )

        assertEquals("payload1", instruction.payloadKey)
        assertEquals(PayloadOperationType.AppendOrOverwrite, instruction.operationType)
        assertEquals("text/plain", instruction.contentType)
    }

    // ========== FileIdFileIdentifier Tests ==========

    @Test
    fun `FileIdFileIdentifier creation works correctly`() {
        val identifier = FileIdFileIdentifier(fileId = "file-123", targetDrive = testTargetDrive)

        assertEquals("file-123", identifier.fileId)
        assertEquals(testTargetDrive, identifier.targetDrive)
    }

    // ========== UpdateLocale Tests ==========

    @Test
    fun `UpdateLocale enum values exist`() {
        assertEquals(UpdateLocale.Peer, UpdateLocale.valueOf("Peer"))
        assertEquals(UpdateLocale.Local, UpdateLocale.valueOf("Local"))
    }
}
