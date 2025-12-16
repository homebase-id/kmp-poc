package id.homebase.homebasekmppoc.lib.serialization

import id.homebase.homebasekmppoc.prototype.lib.drives.AccessControlList
import id.homebase.homebasekmppoc.prototype.lib.drives.FileState
import id.homebase.homebasekmppoc.prototype.lib.drives.FileSystemType
import id.homebase.homebasekmppoc.prototype.lib.drives.ServerMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.drives.files.AppFileMetaData
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ArchivalStatus
import id.homebase.homebasekmppoc.prototype.lib.drives.files.CommentPreview
import id.homebase.homebasekmppoc.prototype.lib.drives.files.DataSource
import id.homebase.homebasekmppoc.prototype.lib.drives.files.FileMetadata
import id.homebase.homebasekmppoc.prototype.lib.drives.files.GlobalTransitIdFileIdentifier
import id.homebase.homebasekmppoc.prototype.lib.drives.files.PayloadDescriptor
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ReactionEntry
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ReactionSummary
import id.homebase.homebasekmppoc.prototype.lib.drives.files.ThumbnailDescriptor
import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

/**
 * Individual serialization tests for each model class in lib/drives using real-world JSON fragments
 * from actual API responses.
 */
@ExperimentalUuidApi
class DriveModelsSerializationTest {

    // ========================================================================
    // TargetDrive Tests
    // ========================================================================

    @Test
    fun testTargetDrive_serialization() {
        val json =
                """
        {
            "alias": "4db49422ebad02e99ab96e9c477d1e08",
            "type": "a3227ffba87608beeb24fee9b70d92a6"
        }
        """.trimIndent()

        val targetDrive = OdinSystemSerializer.deserialize<TargetDrive>(json)

        assertNotNull(targetDrive)
        assertEquals("4db49422-ebad-02e9-9ab9-6e9c477d1e08", targetDrive.alias.toString())
        assertEquals("a3227ffb-a876-08be-eb24-fee9b70d92a6", targetDrive.type.toString())
        assertTrue(targetDrive.isValid())
    }

    // ========================================================================
    // ThumbnailDescriptor Tests
    // ========================================================================

    @Test
    fun testThumbnailDescriptor_serialization() {
        val json =
                """
        {
            "pixelWidth": 320,
            "pixelHeight": 159,
            "contentType": "image/webp",
            "bytesWritten": 13132
        }
        """.trimIndent()

        val thumbnail = OdinSystemSerializer.deserialize<ThumbnailDescriptor>(json)

        assertNotNull(thumbnail)
        assertEquals(320, thumbnail.pixelWidth)
        assertEquals(159, thumbnail.pixelHeight)
        assertEquals("image/webp", thumbnail.contentType)
        assertEquals(13132L, thumbnail.bytesWritten)
    }

    @Test
    fun testThumbnailDescriptor_withBase64Content() {
        val json =
                """
        {
            "content": "UklGRugCAABXRUJQVlA4WAoAAAA=",
            "pixelWidth": 2912,
            "pixelHeight": 1440,
            "contentType": "image/webp",
            "bytesWritten": 0
        }
        """.trimIndent()

        val thumbnail = OdinSystemSerializer.deserialize<ThumbnailDescriptor>(json)

        assertNotNull(thumbnail)
        assertNotNull(thumbnail.content)
        assertTrue(thumbnail.content!!.startsWith("UklGRug"))
        assertEquals(2912, thumbnail.pixelWidth)
    }

    // ========================================================================
    // PayloadDescriptor Tests
    // ========================================================================

    @Test
    fun testPayloadDescriptor_serialization() {
        val json =
                """
        {
            "iv": null,
            "key": "pst_mdi0",
            "contentType": "image/jpeg",
            "bytesWritten": 473932,
            "lastModified": 1764019518116,
            "descriptorContent": null,
            "previewThumbnail": {
                "content": "UklGRugCAABXRUJQVlA4WAoAAAA=",
                "pixelWidth": 2912,
                "pixelHeight": 1440,
                "contentType": "image/webp",
                "bytesWritten": 0
            },
            "thumbnails": [
                {
                    "pixelWidth": 20,
                    "pixelHeight": 10,
                    "contentType": "image/webp",
                    "bytesWritten": 752
                },
                {
                    "pixelWidth": 320,
                    "pixelHeight": 159,
                    "contentType": "image/webp",
                    "bytesWritten": 13132
                }
            ],
            "uid": 115606783129092100
        }
        """.trimIndent()

        val payload = OdinSystemSerializer.deserialize<PayloadDescriptor>(json)

        assertNotNull(payload)
        assertEquals("pst_mdi0", payload.key)
        assertEquals("image/jpeg", payload.contentType)
        assertEquals(473932L, payload.bytesWritten)
        assertEquals(1764019518116L, payload.lastModified)
        assertNotNull(payload.previewThumbnail)
        assertEquals(2912, payload.previewThumbnail?.pixelWidth)
        assertNotNull(payload.thumbnails)
        assertEquals(2, payload.thumbnails?.size)
        assertEquals(20, payload.thumbnails?.get(0)?.pixelWidth)
        assertEquals(115606783129092100L, payload.uid)
    }

    // ========================================================================
    // DataSource Tests
    // ========================================================================

    @Test
    fun testDataSource_serialization() {
        val json =
                """
        {
            "identity": "bishwajeetparhi.dev",
            "driveId": "44029291-2065-8fa5-c573-cbc2bc03c4a9",
            "payloadsAreRemote": true
        }
        """.trimIndent()

        val dataSource = OdinSystemSerializer.deserialize<DataSource>(json)

        assertNotNull(dataSource)
        assertEquals("bishwajeetparhi.dev", dataSource.identity)
        assertEquals("44029291-2065-8fa5-c573-cbc2bc03c4a9", dataSource.driveId.toString())
        assertTrue(dataSource.payloadsAreRemote)
    }

    // ========================================================================
    // ReactionEntry Tests
    // ========================================================================

    @Test
    fun testReactionEntry_serialization() {
        val json =
                """
        {
            "key": "b2142537-e1c7-c0e9-9304-db75db820d63",
            "reactionContent": "{\"emoji\":\"🤭\"}",
            "count": 1
        }
        """.trimIndent()

        val entry = OdinSystemSerializer.deserialize<ReactionEntry>(json)

        assertNotNull(entry)
        assertEquals("b2142537-e1c7-c0e9-9304-db75db820d63", entry.key)
        assertTrue(entry.reactionContent.contains("emoji"))
        assertEquals(1, entry.count)
    }

    // ========================================================================
    // CommentPreview Tests
    // ========================================================================

    @Test
    fun testCommentPreview_serialization() {
        val json =
                """
        {
            "fileId": "0198ab19-90b3-6a00-b30c-351522133035",
            "odinId": "yagni.dk",
            "content": "{\"authorOdinId\":\"yagni.dk\",\"body\":\"I'm too old. I don't understand what it does  😅\"}",
            "reactions": [],
            "created": 1764048771901,
            "updated": 1764048771901,
            "isEncrypted": false
        }
        """.trimIndent()

        val comment = OdinSystemSerializer.deserialize<CommentPreview>(json)

        assertNotNull(comment)
        assertEquals("0198ab19-90b3-6a00-b30c-351522133035", comment.fileId)
        assertEquals("yagni.dk", comment.odinId)
        assertTrue(comment.content.contains("I'm too old"))
        assertEquals(1764048771901L, comment.created)
        assertEquals(1764048771901L, comment.updated)
        assertEquals(false, comment.isEncrypted)
        assertTrue(comment.reactions.isEmpty())
    }

    // ========================================================================
    // ReactionSummary Tests
    // ========================================================================

    @Test
    fun testReactionSummary_serialization() {
        val json =
                """
        {
            "reactions": {
                "b2142537-e1c7-c0e9-9304-db75db820d63": {
                    "key": "b2142537-e1c7-c0e9-9304-db75db820d63",
                    "reactionContent": "{\"emoji\":\"🤭\"}",
                    "count": 1
                },
                "f9c78caa-e6b9-4b04-8551-cd45e49f98eb": {
                    "key": "f9c78caa-e6b9-4b04-8551-cd45e49f98eb",
                    "reactionContent": "{\"emoji\":\"❤️\"}",
                    "count": 1
                }
            },
            "comments": [
                {
                    "fileId": "0198ab19-90b3-6a00-b30c-351522133035",
                    "odinId": "yagni.dk",
                    "content": "{\"authorOdinId\":\"yagni.dk\",\"body\":\"First comment\"}",
                    "reactions": [],
                    "created": 1764048771901,
                    "updated": 1764048771901,
                    "isEncrypted": false
                },
                {
                    "fileId": "0a98ab19-c0fd-9b00-1666-2f97f448b677",
                    "odinId": "yagni.dk",
                    "content": "{\"authorOdinId\":\"yagni.dk\",\"body\":\"Second comment\"}",
                    "reactions": [],
                    "created": 1764048809952,
                    "updated": 1764048809952,
                    "isEncrypted": false
                }
            ],
            "totalCommentCount": 2
        }
        """.trimIndent()

        val summary = OdinSystemSerializer.deserialize<ReactionSummary>(json)

        assertNotNull(summary)
        assertEquals(2, summary.reactions.size)
        assertTrue(summary.reactions.containsKey("b2142537-e1c7-c0e9-9304-db75db820d63"))
        assertEquals(2, summary.comments.size)
        assertEquals("yagni.dk", summary.comments[0].odinId)
        assertEquals(2, summary.totalCommentCount)
    }

    // ========================================================================
    // AccessControlList Tests
    // ========================================================================

    @Test
    fun testAccessControlList_serialization() {
        val json =
                """
        {
            "requiredSecurityGroup": "owner",
            "circleIdList": null,
            "odinIdList": null
        }
        """.trimIndent()

        val acl = OdinSystemSerializer.deserialize<AccessControlList>(json)

        assertNotNull(acl)
        assertEquals("owner", acl.requiredSecurityGroup)
        assertEquals(null, acl.circleIdList)
        assertEquals(null, acl.odinIdList)
    }

    @Test
    fun testAccessControlList_withLists() {
        val json =
                """
        {
            "requiredSecurityGroup": "connected",
            "circleIdList": ["circle1", "circle2"],
            "odinIdList": ["user1.odin", "user2.odin"]
        }
        """.trimIndent()

        val acl = OdinSystemSerializer.deserialize<AccessControlList>(json)

        assertNotNull(acl)
        assertEquals("connected", acl.requiredSecurityGroup)
        assertEquals(2, acl.circleIdList?.size)
        assertEquals(2, acl.odinIdList?.size)
    }

    // ========================================================================
    // ServerMetadata Tests
    // ========================================================================

    @Test
    fun testServerMetadata_serialization() {
        val json =
                """
        {
            "accessControlList": {
                "requiredSecurityGroup": "owner",
                "circleIdList": null,
                "odinIdList": null
            },
            "doNotIndex": false,
            "allowDistribution": false,
            "fileSystemType": "standard",
            "fileByteCount": 9007,
            "originalRecipientCount": 0,
            "transferHistory": null
        }
        """.trimIndent()

        val metadata = OdinSystemSerializer.deserialize<ServerMetadata>(json)

        assertNotNull(metadata)
        assertNotNull(metadata.accessControlList)
        assertEquals("owner", metadata.accessControlList?.requiredSecurityGroup)
        assertEquals(false, metadata.allowDistribution)
        assertEquals(FileSystemType.Standard, metadata.fileSystemType)
        assertEquals(9007L, metadata.fileByteCount)
        assertEquals(0, metadata.originalRecipientCount)
    }

    // ========================================================================
    // AppFileMetaData Tests
    // ========================================================================

    @Test
    fun testAppFileMetaData_serialization() {
        val json =
                """
        {
            "uniqueId": null,
            "tags": [
                "32e861cf-bf6c-1eaf-3d57-efb2258b7fcc"
            ],
            "fileType": 101,
            "dataType": 100,
            "groupId": null,
            "userDate": 1764019517491,
            "content": "Simple text content",
            "previewThumbnail": {
                "content": "UklGRugCAABXRUJQVlA4WAoAAAA=",
                "pixelWidth": 2912,
                "pixelHeight": 1440,
                "contentType": "image/webp",
                "bytesWritten": 0
            },
            "archivalStatus": 0
        }
        """.trimIndent()

        val appData = OdinSystemSerializer.deserialize<AppFileMetaData>(json)

        assertNotNull(appData)
        assertEquals(null, appData.uniqueId)
        assertNotNull(appData.tags)
        assertEquals(1, appData.tags?.size)
        assertEquals("32e861cf-bf6c-1eaf-3d57-efb2258b7fcc", appData.tags?.get(0).toString())
        assertEquals(101, appData.fileType)
        assertEquals(100, appData.dataType)
        assertEquals(1764019517491L, appData.userDate)
        assertEquals("Simple text content", appData.content)
        assertNotNull(appData.previewThumbnail)
        assertEquals(2912, appData.previewThumbnail?.pixelWidth)
        assertEquals(ArchivalStatus.None, appData.archivalStatus)
    }

    // ========================================================================
    // GlobalTransitIdFileIdentifier Tests
    // ========================================================================

    @Test
    fun testGlobalTransitIdFileIdentifier_serialization() {
        val json =
                """
        {
            "targetDrive": {
                "alias": "4db49422ebad02e99ab96e9c477d1e08",
                "type": "a3227ffba87608beeb24fee9b70d92a6"
            },
            "globalTransitId": "d3faaaf3-9c75-4fe3-9a4f-cee91ab3d667"
        }
        """.trimIndent()

        val identifier = OdinSystemSerializer.deserialize<GlobalTransitIdFileIdentifier>(json)

        assertNotNull(identifier)
        assertEquals("d3faaaf3-9c75-4fe3-9a4f-cee91ab3d667", identifier.globalTransitId.toString())
        assertNotNull(identifier.targetDrive)
        assertNotNull(identifier.targetDrive)
    }

    // ========================================================================
    // ClientFileMetadata Tests (subset without complex content)
    // ========================================================================

    @Test
    fun testClientFileMetadata_serialization() {
        val json =
                """
        {
            "globalTransitId": "d3faaaf3-9c75-4fe3-9a4f-cee91ab3d667",
            "created": 1764019518656,
            "updated": 1764166816354,
            "transitCreated": 0,
            "transitUpdated": 0,
            "isEncrypted": false,
            "senderOdinId": "bishwajeetparhi.dev",
            "originalAuthor": "bishwajeetparhi.dev",
            "appData": {
                "tags": ["32e861cf-bf6c-1eaf-3d57-efb2258b7fcc"],
                "fileType": 101,
                "dataType": 100,
                "archivalStatus": 0
            },
            "versionTag": "9508ac19-e025-d600-f8a8-429648bd776c",
            "payloads": [
                {
                    "key": "pst_mdi0",
                    "contentType": "image/jpeg",
                    "bytesWritten": 473932
                }
            ],
            "dataSource": {
                "identity": "bishwajeetparhi.dev",
                "driveId": "44029291-2065-8fa5-c573-cbc2bc03c4a9",
                "payloadsAreRemote": true
            }
        }
        """.trimIndent()

        val metadata = OdinSystemSerializer.deserialize<FileMetadata>(json)

        assertNotNull(metadata)
        assertEquals("d3faaaf3-9c75-4fe3-9a4f-cee91ab3d667", metadata.globalTransitId.toString())
        assertEquals("bishwajeetparhi.dev", metadata.senderOdinId)
        assertEquals("bishwajeetparhi.dev", metadata.originalAuthor)
        assertEquals(false, metadata.isEncrypted)
        assertNotNull(metadata.appData)
        assertEquals(101, metadata.appData.fileType)
        assertNotNull(metadata.payloads)
        assertEquals(1, metadata.payloads?.size)
        assertEquals("pst_mdi0", metadata.payloads?.get(0)?.key)
        assertNotNull(metadata.dataSource)
        assertEquals("bishwajeetparhi.dev", metadata.dataSource?.identity)
    }

    // ========================================================================
    // FileState and FileSystemType Enum Tests
    // ========================================================================

    @Test
    fun testFileState_serialization() {
        val activeJson = "\"active\""
        val deletedJson = "\"deleted\""

        val active = OdinSystemSerializer.deserialize<FileState>(activeJson)
        val deleted = OdinSystemSerializer.deserialize<FileState>(deletedJson)

        assertEquals(FileState.Active, active)
        assertEquals(FileState.Deleted, deleted)
    }

    @Test
    fun testFileSystemType_serialization() {
        val standardJson = "\"standard\""
        val commentJson = "\"comment\""

        val standard = OdinSystemSerializer.deserialize<FileSystemType>(standardJson)
        val comment = OdinSystemSerializer.deserialize<FileSystemType>(commentJson)

        assertEquals(FileSystemType.Standard, standard)
        assertEquals(FileSystemType.Comment, comment)
    }

    @Test
    fun testArchivalStatus_serialization() {
        val noneJson = "0"
        val archivedJson = "1"
        val removedJson = "2"

        val none = OdinSystemSerializer.deserialize<ArchivalStatus>(noneJson)
        val archived = OdinSystemSerializer.deserialize<ArchivalStatus>(archivedJson)
        val removed = OdinSystemSerializer.deserialize<ArchivalStatus>(removedJson)

        assertEquals(ArchivalStatus.None, none)
        assertEquals(ArchivalStatus.Archived, archived)
        assertEquals(ArchivalStatus.Removed, removed)
    }
}
