package id.homebase.homebasekmppoc.serialization

import id.homebase.homebasekmppoc.drives.SharedSecretEncryptedFileHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class SharedSecretEncryptedFileHeaderTest {

    @Test
    fun testDeserialize_fullFileHeader() {
        val json = """
{
    "fileId": "1355aa19-2030-8200-00ef-563eed96bebf",
    "targetDrive": {
        "alias": "e8475dc46cb4b6651c2d0dbd0f3aad5f",
        "type": "8f448716e34cedf9014145e043ca6612"
    },
    "fileState": "active",
    "fileSystemType": "standard",
    "sharedSecretEncryptedKeyHeader": {
        "encryptionVersion": 1,
        "type": "aes",
        "iv": "fA2HYW8SoHnP3oMxgPcckA==",
        "encryptedAesKey": "lCGJ4kL+OC2I+Q1YIvkTVU/GUpmVHAMA+axkwZQJxu5tGHAQd2CLzEzGX0X2pcyE"
    },
    "fileMetadata": {
        "globalTransitId": "ae9f4cea-65f0-43e0-adb9-bfd2ce3cc1d5",
        "created": 1763710153491,
        "updated": 1763710153491,
        "transitCreated": 0,
        "transitUpdated": 0,
        "isEncrypted": true,
        "senderOdinId": "frodo.dotyou.cloud",
        "originalAuthor": "frodo.dotyou.cloud",
        "appData": {
            "uniqueId": "00c7b92f-246d-6b66-29e7-53f94679873d",
            "tags": [
                "bdaef89a-f262-8bd2-554f-380c4537e0e5"
            ],
            "fileType": 101,
            "dataType": 100,
            "groupId": null,
            "userDate": 1763710152667,
            "content": "test content",
            "archivalStatus": 0
        },
        "localAppData": null,
        "referencedFile": null,
        "reactionPreview": null,
        "versionTag": "1355aa19-2031-d800-403d-e8696a8be494",
        "payloads": [
            {
                "iv": "3zvsfQ3qQbyup44mm1BAfw==",
                "key": "pst_mdi0",
                "contentType": "image/jpeg",
                "bytesWritten": 5082368,
                "lastModified": 1763710153486,
                "descriptorContent": null,
                "uid": 115586508618072064
            }
        ],
        "dataSource": null
    },
    "serverMetadata": {
        "accessControlList": {
            "requiredSecurityGroup": "connected",
            "circleIdList": null,
            "odinIdList": null
        },
        "doNotIndex": false,
        "allowDistribution": true,
        "fileSystemType": "standard",
        "fileByteCount": 5402950,
        "originalRecipientCount": 0,
        "transferHistory": null
    },
    "priority": 300,
    "fileByteCount": 5402950
}
        """.trimIndent()

        val header = OdinSystemSerializer.deserialize<SharedSecretEncryptedFileHeader>(json)

        assertNotNull(header)
        assertEquals("1355aa19-2030-8200-00ef-563eed96bebf", header.fileId.toString())
        assertEquals("e8475dc4-6cb4-b665-1c2d-0dbd0f3aad5f", header.targetDrive.alias.toString())
        assertEquals(300, header.priority)
        assertEquals(5402950L, header.fileByteCount)

        // Verify enum deserialization
        assertEquals(id.homebase.homebasekmppoc.drives.FileState.Active, header.fileState)
        assertEquals(id.homebase.homebasekmppoc.drives.FileSystemType.Standard, header.fileSystemType)

        // Verify Base64 ByteArray deserialization
        assertNotNull(header.sharedSecretEncryptedKeyHeader.iv)
        assertNotNull(header.sharedSecretEncryptedKeyHeader.encryptedAesKey)

        // Verify nested metadata
        assertEquals("frodo.dotyou.cloud", header.fileMetadata.senderOdinId)
        assertEquals(1, header.fileMetadata.payloads?.size)
        assertEquals("pst_mdi0", header.fileMetadata.payloads?.get(0)?.key)
    }
}
