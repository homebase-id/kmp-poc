package id.homebase.homebasekmppoc.prototype.lib.drives.files

import id.homebase.homebasekmppoc.prototype.lib.crypto.EncryptedKeyHeader
import id.homebase.homebasekmppoc.prototype.lib.drives.ArchivalStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.ClientFileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Unit tests for File Types in drives/files */
class FileTypesTest {

    // Test data
    private val testTargetDrive =
            TargetDrive(
                    alias = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                    type = Uuid.parse("00000000-0000-0000-0000-000000000002")
            )

    private val testFileId = Uuid.parse("00000000-0000-0000-0000-000000000003")

    // ========== SecurityGroupType Tests ==========

    @Test
    fun `SecurityGroupType values are correct`() {
        assertEquals("anonymous", SecurityGroupType.Anonymous.value)
        assertEquals("authenticated", SecurityGroupType.Authenticated.value)
        assertEquals("connected", SecurityGroupType.Connected.value)
        assertEquals("autoconnected", SecurityGroupType.AutoConnected.value)
        assertEquals("owner", SecurityGroupType.Owner.value)
    }

    @Test
    fun `SecurityGroupType fromString works correctly`() {
        assertEquals(SecurityGroupType.Anonymous, SecurityGroupType.fromString("anonymous"))
        assertEquals(SecurityGroupType.Authenticated, SecurityGroupType.fromString("AUTHENTICATED"))
        assertEquals(SecurityGroupType.Connected, SecurityGroupType.fromString("connected"))
        assertEquals(SecurityGroupType.Owner, SecurityGroupType.fromString("owner"))
        assertEquals(
                SecurityGroupType.Anonymous,
                SecurityGroupType.fromString("unknown")
        ) // Default
    }

    // ========== TransferStatus Tests ==========

    @Test
    fun `TransferStatus values are correct`() {
        assertEquals("none", TransferStatus.None.value)
        assertEquals("delivered", TransferStatus.Delivered.value)
        assertEquals(
                "recipientidentityreturnedaccessdenied",
                TransferStatus.RecipientIdentityReturnedAccessDenied.value
        )
    }

    @Test
    fun `TransferStatus fromString works correctly`() {
        assertEquals(TransferStatus.None, TransferStatus.fromString("none"))
        assertEquals(TransferStatus.Delivered, TransferStatus.fromString("DELIVERED"))
        assertEquals(
                TransferStatus.UnknownServerError,
                TransferStatus.fromString("unknownservererror")
        )
        assertEquals(TransferStatus.None, TransferStatus.fromString("unknown")) // Default
    }

    @Test
    fun `TransferStatus failedStatuses contains all failure statuses`() {
        assertEquals(7, TransferStatus.failedStatuses.size)
        assertTrue(
                TransferStatus.RecipientIdentityReturnedAccessDenied in
                        TransferStatus.failedStatuses
        )
        assertTrue(
                TransferStatus.SourceFileDoesNotAllowDistribution in TransferStatus.failedStatuses
        )
        assertTrue(TransferStatus.RecipientServerNotResponding in TransferStatus.failedStatuses)
        assertTrue(TransferStatus.UnknownServerError in TransferStatus.failedStatuses)
        assertFalse(TransferStatus.None in TransferStatus.failedStatuses)
        assertFalse(TransferStatus.Delivered in TransferStatus.failedStatuses)
    }

    @Test
    fun `TransferStatus isFailedStatus works correctly`() {
        assertTrue(
                TransferStatus.isFailedStatus(TransferStatus.RecipientIdentityReturnedAccessDenied)
        )
        assertTrue(TransferStatus.isFailedStatus(TransferStatus.UnknownServerError))
        assertFalse(TransferStatus.isFailedStatus(TransferStatus.None))
        assertFalse(TransferStatus.isFailedStatus(TransferStatus.Delivered))
    }

    // ========== RichText Tests ==========

    @Test
    fun `RichTextNode creation works correctly`() {
        val node =
                RichTextNode(
                        type = "paragraph",
                        id = "p1",
                        text = "Hello World",
                        children = listOf(RichTextNode(type = "bold", text = "Bold text"))
                )

        assertEquals("paragraph", node.type)
        assertEquals("p1", node.id)
        assertEquals("Hello World", node.text)
        assertEquals(1, node.children?.size)
        assertEquals("bold", node.children?.first()?.type)
    }

    @Test
    fun `CommentReaction creation works correctly`() {
        val richText: RichText = listOf(RichTextNode(type = "text", text = "Hello"))

        val reaction =
                CommentReaction(
                        authorOdinId = "user@example.com",
                        body = "Great post!",
                        bodyAsRichText = richText,
                        mediaPayloadKey = "media-123"
                )

        assertEquals("user@example.com", reaction.authorOdinId)
        assertEquals("Great post!", reaction.body)
        assertEquals(1, reaction.bodyAsRichText?.size)
        assertEquals("media-123", reaction.mediaPayloadKey)
    }

    // ========== TransferHistory Tests ==========

    @Test
    fun `RecipientTransferSummary creation works correctly`() {
        val summary =
                RecipientTransferSummary(
                        totalInOutbox = 5,
                        totalFailed = 2,
                        totalDelivered = 10,
                        totalReadByRecipient = 8
                )

        assertEquals(5, summary.totalInOutbox)
        assertEquals(2, summary.totalFailed)
        assertEquals(10, summary.totalDelivered)
        assertEquals(8, summary.totalReadByRecipient)
    }

    @Test
    fun `RecipientTransferHistoryEntry creation works correctly`() {
        val entry =
                RecipientTransferHistoryEntry(
                        recipient = "user@example.com",
                        lastUpdated = 1702656000L,
                        latestTransferStatus = TransferStatus.Delivered,
                        isInOutbox = "false",
                        latestSuccessfullyDeliveredVersionTag = "v1",
                        isReadByRecipient = true
                )

        assertEquals("user@example.com", entry.recipient)
        assertEquals(TransferStatus.Delivered, entry.latestTransferStatus)
        assertTrue(entry.isReadByRecipient)
    }

    // ========== HomebaseFile Tests ==========

    @Test
    fun `HomebaseFileState values are correct`() {
        assertEquals("active", HomebaseFileState.Active.value)
        assertEquals("deleted", HomebaseFileState.Deleted.value)
    }

    @Test
    fun `HomebaseFileState fromString works correctly`() {
        assertEquals(HomebaseFileState.Active, HomebaseFileState.fromString("active"))
        assertEquals(HomebaseFileState.Deleted, HomebaseFileState.fromString("DELETED"))
        assertEquals(HomebaseFileState.Active, HomebaseFileState.fromString("unknown")) // Default
    }

    @Test
    fun `HomebaseFile isActive and isDeleted work correctly`() {
        val activeFile =
                HomebaseFile(
                        fileId = testFileId,
                        fileSystemType = FileSystemType.Standard,
                        fileState = HomebaseFileState.Active,
                        fileMetadata = ClientFileMetadata(),
                        sharedSecretEncryptedKeyHeader = EncryptedKeyHeader.empty()
                )

        assertTrue(activeFile.isActive)
        assertFalse(activeFile.isDeleted)

        val deletedFile = activeFile.copy(fileState = HomebaseFileState.Deleted)

        assertFalse(deletedFile.isActive)
        assertTrue(deletedFile.isDeleted)
    }

    // ========== FileIdentifiers Tests ==========

    @Test
    fun `FileIdFileIdentifier creation works correctly`() {
        val identifier = FileIdFileIdentifier(fileId = testFileId, targetDrive = testTargetDrive)

        assertEquals(testFileId, identifier.fileId)
        assertEquals(testTargetDrive, identifier.targetDrive)
    }

    @Test
    fun `GlobalTransitIdFileIdentifier creation works correctly`() {
        val gtid = Uuid.parse("00000000-0000-0000-0000-000000000004")
        val identifier =
                GlobalTransitIdFileIdentifier(globalTransitId = gtid, targetDrive = testTargetDrive)

        assertEquals(gtid, identifier.globalTransitId)
        assertEquals(testTargetDrive, identifier.targetDrive)
    }

    @Test
    fun `UniqueIdFileIdentifier creation works correctly`() {
        val uniqueId = Uuid.parse("00000000-0000-0000-0000-000000000005")
        val identifier = UniqueIdFileIdentifier(uniqueId = uniqueId, targetDrive = testTargetDrive)

        assertEquals(uniqueId, identifier.uniqueId)
        assertEquals(testTargetDrive, identifier.targetDrive)
    }

    @Test
    fun `FileIdentifierUnion sealed class works correctly`() {
        val fileIdIdentifier = FileIdFileIdentifier(testFileId, testTargetDrive)
        val union: FileIdentifierUnion = FileIdentifierUnion.ByFileId(fileIdIdentifier)

        assertEquals(testTargetDrive, union.targetDrive)

        // Test that we can extract the identifier from the union
        val byFileId = union as FileIdentifierUnion.ByFileId
        assertEquals(testFileId, byFileId.identifier.fileId)
    }

    // ========== MediaFile Tests ==========

    @Test
    fun `MediaFile creation works correctly`() {
        val mediaFile =
                MediaFile(fileId = "file-123", key = "payload-key", contentType = "image/jpeg")

        assertEquals("file-123", mediaFile.fileId)
        assertEquals("payload-key", mediaFile.key)
        assertEquals("image/jpeg", mediaFile.contentType)
    }

    @Test
    fun `PayloadFile creation works correctly`() {
        val testPayload = byteArrayOf(1, 2, 3, 4)
        val payloadFile =
                PayloadFile(
                        key = "payload-1",
                        payload = testPayload,
                        descriptorContent = "description",
                        skipEncryption = false
                )

        assertEquals("payload-1", payloadFile.key)
        assertTrue(testPayload.contentEquals(payloadFile.payload))
        assertEquals("description", payloadFile.descriptorContent)
        assertFalse(payloadFile.skipEncryption)
    }

    @Test
    fun `PayloadFile with manual encryption sets iv`() {
        val testPayload = byteArrayOf(1, 2, 3, 4)
        val testIv = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        val payloadFile =
                PayloadFile(
                        key = "payload-1",
                        payload = testPayload,
                        skipEncryption = true,
                        iv = testIv
                )

        assertTrue(payloadFile.skipEncryption)
        assertTrue(testIv.contentEquals(payloadFile.iv!!))
    }

    @Test
    fun `PayloadEmbeddedThumb creation works correctly`() {
        val thumb =
                PayloadEmbeddedThumb(
                        pixelWidth = 100,
                        pixelHeight = 100,
                        contentType = "image/webp",
                        content = "base64encodedcontent"
                )

        assertEquals(100, thumb.pixelWidth)
        assertEquals(100, thumb.pixelHeight)
        assertEquals("image/webp", thumb.contentType)
        assertEquals("base64encodedcontent", thumb.content)
    }

    // ========== NewHomebaseFile Tests ==========

    @Test
    fun `NewAppFileMetaData creation works correctly`() {
        val appData =
                NewAppFileMetaData(
                        content = "Test content",
                        fileType = 100,
                        dataType = 200,
                        tags = listOf("tag1", "tag2"),
                        archivalStatus = ArchivalStatus.None
                )

        assertEquals("Test content", appData.content)
        assertEquals(100, appData.fileType)
        assertEquals(200, appData.dataType)
        assertEquals(listOf("tag1", "tag2"), appData.tags)
        assertEquals(ArchivalStatus.None, appData.archivalStatus)
    }

    @Test
    fun `NewFileMetadata creation works correctly`() {
        val appData = NewAppFileMetaData(content = "Test")
        val metadata =
                NewFileMetadata(
                        isEncrypted = true,
                        originalAuthor = "author@example.com",
                        appData = appData,
                        versionTag = "v1"
                )

        assertTrue(metadata.isEncrypted)
        assertEquals("author@example.com", metadata.originalAuthor)
        assertEquals("v1", metadata.versionTag)
        assertEquals("Test", metadata.appData.content)
    }

    @Test
    fun `FileAccessControlList creation with SecurityGroupType works`() {
        val acl =
                FileAccessControlList(
                        requiredSecurityGroup = SecurityGroupType.Connected,
                        circleIdList = listOf("circle1", "circle2"),
                        odinIdList = listOf("user1@example.com")
                )

        assertEquals(SecurityGroupType.Connected, acl.requiredSecurityGroup)
        assertEquals(2, acl.circleIdList?.size)
        assertEquals(1, acl.odinIdList?.size)
    }

    @Test
    fun `UploadProgress creation works correctly`() {
        val progress = UploadProgress(phase = "uploading", progress = 0.75)

        assertEquals("uploading", progress.phase)
        assertEquals(0.75, progress.progress)
    }

    @Test
    fun `NewHomebaseFile creation works correctly`() {
        val appData = NewAppFileMetaData(content = "New file content")
        val metadata = NewFileMetadata(appData = appData)
        val newFile =
                NewHomebaseFile(
                        fileId = testFileId,
                        fileSystemType = "Standard",
                        fileMetadata = metadata,
                        serverMetadata =
                                NewServerMetaData(
                                        accessControlList =
                                                FileAccessControlList(
                                                        requiredSecurityGroup =
                                                                SecurityGroupType.Owner
                                                )
                                )
                )

        assertEquals(testFileId, newFile.fileId)
        assertEquals("Standard", newFile.fileSystemType)
        assertEquals("New file content", newFile.fileMetadata.appData.content)
        assertEquals(
                SecurityGroupType.Owner,
                newFile.serverMetadata?.accessControlList?.requiredSecurityGroup
        )
    }
}
