package id.homebase.homebasekmppoc.prototype.lib.core

import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Unit tests for OdinException classes and OdinClientErrorCode serialization. */
class OdinExceptionTest {

    @Test
    fun testOdinClientErrorCode_fromInt_knownValue() {
        val errorCode = OdinClientErrorCode.fromInt(4160)
        assertEquals(OdinClientErrorCode.VersionTagMismatch, errorCode)
    }

    @Test
    fun testOdinClientErrorCode_fromInt_unknownValue() {
        val errorCode = OdinClientErrorCode.fromInt(999999)
        assertEquals(OdinClientErrorCode.UnhandledScenario, errorCode)
    }

    @Test
    fun testOdinClientErrorCode_fromString_pascalCase() {
        val errorCode = OdinClientErrorCode.fromString("VersionTagMismatch")
        assertEquals(OdinClientErrorCode.VersionTagMismatch, errorCode)
    }

    @Test
    fun testOdinClientErrorCode_fromString_camelCase() {
        val errorCode = OdinClientErrorCode.fromString("versionTagMismatch")
        assertEquals(OdinClientErrorCode.VersionTagMismatch, errorCode)
    }

    @Test
    fun testOdinClientErrorCode_fromString_unknownValue() {
        val errorCode = OdinClientErrorCode.fromString("unknownError")
        assertEquals(OdinClientErrorCode.UnhandledScenario, errorCode)
    }

    @Test
    fun testOdinClientErrorCode_serialization_toJson() {
        val json = OdinSystemSerializer.json
        val serialized =
                json.encodeToString(
                        OdinClientErrorCode.serializer(),
                        OdinClientErrorCode.FileNotFound
                )
        // Should serialize to camelCase string
        assertEquals("\"fileNotFound\"", serialized)
    }

    @Test
    fun testOdinClientErrorCode_deserialization_fromString() {
        val json = OdinSystemSerializer.json
        val deserialized =
                json.decodeFromString(OdinClientErrorCode.serializer(), "\"VersionTagMismatch\"")
        assertEquals(OdinClientErrorCode.VersionTagMismatch, deserialized)
    }

    @Test
    fun testOdinClientErrorCode_deserialization_fromInt() {
        val json = OdinSystemSerializer.json
        val deserialized = json.decodeFromString(OdinClientErrorCode.serializer(), "4106")
        assertEquals(OdinClientErrorCode.FileNotFound, deserialized)
    }

    @Test
    fun testOdinClientErrorCode_deserialization_unknownString() {
        val json = OdinSystemSerializer.json
        val deserialized =
                json.decodeFromString(OdinClientErrorCode.serializer(), "\"unknownCode\"")
        assertEquals(OdinClientErrorCode.UnhandledScenario, deserialized)
    }

    @Test
    fun testOdinErrorResponse_deserialization_withStringErrorCode() {
        val json = OdinSystemSerializer.json
        val jsonString = """{"errorCode": "FileNotFound", "message": "The file was not found"}"""
        val response = json.decodeFromString<OdinErrorResponse>(jsonString)

        assertEquals(OdinClientErrorCode.FileNotFound, response.errorCode)
        assertEquals("The file was not found", response.message)
    }

    @Test
    fun testOdinErrorResponse_deserialization_withIntErrorCode() {
        val json = OdinSystemSerializer.json
        val jsonString = """{"errorCode": 4160, "message": "Version mismatch error"}"""
        val response = json.decodeFromString<OdinErrorResponse>(jsonString)

        assertEquals(OdinClientErrorCode.VersionTagMismatch, response.errorCode)
        assertEquals("Version mismatch error", response.message)
    }

    @Test
    fun testOdinErrorResponse_deserialization_nullErrorCode() {
        val json = OdinSystemSerializer.json
        val jsonString = """{"message": "Some error without code"}"""
        val response = json.decodeFromString<OdinErrorResponse>(jsonString)

        assertNull(response.errorCode)
        assertEquals("Some error without code", response.message)
    }

    @Test
    fun testOdinErrorResponse_deserialization_emptyObject() {
        val json = OdinSystemSerializer.json
        val jsonString = """{}"""
        val response = json.decodeFromString<OdinErrorResponse>(jsonString)

        assertNull(response.errorCode)
        assertNull(response.message)
    }

    @Test
    fun testOdinClientException_creation() {
        val exception =
                OdinClientException(
                        message = "Test error message",
                        errorCode = OdinClientErrorCode.FileNotFound
                )

        assertEquals("Test error message", exception.message)
        assertEquals(OdinClientErrorCode.FileNotFound, exception.errorCode)
    }

    @Test
    fun testOdinClientException_defaultErrorCode() {
        val exception = OdinClientException("Default error")

        assertEquals(OdinClientErrorCode.UnhandledScenario, exception.errorCode)
    }

    @Test
    fun testOdinClientException_withCause() {
        val cause = RuntimeException("Root cause")
        val exception =
                OdinClientException(
                        message = "Wrapped error",
                        errorCode = OdinClientErrorCode.InvalidFile,
                        cause = cause
                )

        assertEquals("Wrapped error", exception.message)
        assertEquals(OdinClientErrorCode.InvalidFile, exception.errorCode)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testOdinRemoteIdentityException_creation() {
        val exception =
                OdinRemoteIdentityException(
                        message = "Remote error",
                        errorCode = OdinClientErrorCode.RemoteServerReturnedForbidden
                )

        assertEquals("Remote error", exception.message)
        assertEquals(OdinClientErrorCode.RemoteServerReturnedForbidden, exception.errorCode)
    }

    @Test
    fun testOdinException_isBaseClass() {
        val odinException: OdinException =
                OdinClientException("Test", OdinClientErrorCode.FileNotFound)

        assertNotNull(odinException)
        assertEquals("Test", odinException.message)
    }

    @Test
    fun testOdinClientErrorCode_allDriveErrors() {
        // Verify common drive error codes have correct values
        assertEquals(4106, OdinClientErrorCode.FileNotFound.value)
        assertEquals(4160, OdinClientErrorCode.VersionTagMismatch.value)
        assertEquals(4161, OdinClientErrorCode.InvalidFile.value)
        assertEquals(4163, OdinClientErrorCode.InvalidUpload.value)
        assertEquals(4118, OdinClientErrorCode.UnknownId.value)
    }

    @Test
    fun testOdinClientErrorCode_allAuthErrors() {
        // Verify auth error codes
        assertEquals(1001, OdinClientErrorCode.InvalidAuthToken.value)
        assertEquals(1002, OdinClientErrorCode.SharedSecretEncryptionIsInvalid.value)
    }

    @Test
    fun testOdinClientErrorCode_allTransitErrors() {
        // Verify transit error codes
        assertEquals(7403, OdinClientErrorCode.RemoteServerReturnedForbidden.value)
        assertEquals(7500, OdinClientErrorCode.RemoteServerReturnedInternalServerError.value)
        assertEquals(7905, OdinClientErrorCode.RemoteServerOfflineOrUnavailable.value)
    }
}
