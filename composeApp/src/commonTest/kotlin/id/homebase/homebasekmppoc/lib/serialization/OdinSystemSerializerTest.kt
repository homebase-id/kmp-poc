package id.homebase.homebasekmppoc.lib.serialization

import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
data class TestPerson(
    val FirstName: String,
    val LastName: String,
    val Age: Int,
    val IsActive: Boolean = true
)

@Serializable
data class TestAddress(
    val StreetAddress: String,
    val City: String,
    val PostalCode: String
)

@Serializable
data class TestPersonWithAddress(
    val PersonName: String,
    val Address: TestAddress,
    val ContactNumber: String?
)

class OdinSystemSerializerTest {

    @Test
    fun testSerialize_simpleObject() {
        val person = TestPerson(
            FirstName = "John",
            LastName = "Doe",
            Age = 30,
            IsActive = true
        )

        val json = OdinSystemSerializer.serialize(person)

        // Should convert to camelCase
        assertTrue(json.contains("\"firstName\""))
        assertTrue(json.contains("\"lastName\""))
        assertTrue(json.contains("\"age\""))
        assertTrue(json.contains("\"isActive\""))
        assertTrue(json.contains("\"John\""))
        assertTrue(json.contains("\"Doe\""))
        assertTrue(json.contains("30"))
    }

    @Test
    fun testDeserialize_simpleObject() {
        val json = """{"firstName":"Jane","lastName":"Smith","age":25,"isActive":false}"""

        val person = OdinSystemSerializer.deserialize<TestPerson>(json)

        assertEquals("Jane", person.FirstName)
        assertEquals("Smith", person.LastName)
        assertEquals(25, person.Age)
        assertEquals(false, person.IsActive)
    }

    @Test
    fun testSerializeDeserialize_roundtrip() {
        val original = TestPerson(
            FirstName = "Alice",
            LastName = "Johnson",
            Age = 42,
            IsActive = true
        )

        val json = OdinSystemSerializer.serialize(original)
        val deserialized = OdinSystemSerializer.deserialize<TestPerson>(json)

        assertEquals(original.FirstName, deserialized.FirstName)
        assertEquals(original.LastName, deserialized.LastName)
        assertEquals(original.Age, deserialized.Age)
        assertEquals(original.IsActive, deserialized.IsActive)
    }

    @Test
    fun testSerialize_nestedObject() {
        val personWithAddress = TestPersonWithAddress(
            PersonName = "Bob Brown",
            Address = TestAddress(
                StreetAddress = "123 Main St",
                City = "Springfield",
                PostalCode = "12345"
            ),
            ContactNumber = "555-1234"
        )

        val json = OdinSystemSerializer.serialize(personWithAddress)

        // Check camelCase conversion
        assertTrue(json.contains("\"personName\""))
        assertTrue(json.contains("\"address\""))
        assertTrue(json.contains("\"streetAddress\""))
        assertTrue(json.contains("\"city\""))
        assertTrue(json.contains("\"postalCode\""))
        assertTrue(json.contains("\"contactNumber\""))
    }

    @Test
    fun testDeserialize_nestedObject() {
        val json = """
            {
                "personName":"Charlie",
                "address":{
                    "streetAddress":"456 Oak Ave",
                    "city":"Portland",
                    "postalCode":"67890"
                },
                "contactNumber":"555-5678"
            }
        """.trimIndent()

        val person = OdinSystemSerializer.deserialize<TestPersonWithAddress>(json)

        assertEquals("Charlie", person.PersonName)
        assertEquals("456 Oak Ave", person.Address.StreetAddress)
        assertEquals("Portland", person.Address.City)
        assertEquals("67890", person.Address.PostalCode)
        assertEquals("555-5678", person.ContactNumber)
    }

    @Test
    fun testDeserialize_withDefaultValue() {
        // JSON missing IsActive, should use default value
        val json = """{"firstName":"David","lastName":"Lee","age":35}"""

        val person = OdinSystemSerializer.deserialize<TestPerson>(json)

        assertEquals("David", person.FirstName)
        assertEquals("Lee", person.LastName)
        assertEquals(35, person.Age)
        assertEquals(true, person.IsActive) // Should use default value
    }

    @Test
    fun testDeserialize_withNullValue() {
        val json = """{"personName":"Eve","address":{"streetAddress":"789 Elm","city":"Seattle","postalCode":"98101"},"contactNumber":null}"""

        val person = OdinSystemSerializer.deserialize<TestPersonWithAddress>(json)

        assertEquals("Eve", person.PersonName)
        assertEquals(null, person.ContactNumber)
    }

    @Test
    fun testDeserialize_ignoreUnknownKeys() {
        // JSON has extra field "unknownField" which should be ignored
        val json = """{"firstName":"Frank","lastName":"Miller","age":28,"isActive":true,"unknownField":"extraData"}"""

        val person = OdinSystemSerializer.deserialize<TestPerson>(json)

        assertEquals("Frank", person.FirstName)
        assertEquals("Miller", person.LastName)
        assertEquals(28, person.Age)
    }

    @Test
    fun testDeserialize_invalidJson_throwsException() {
        val invalidJson = """{"firstName":"John","lastName":"""

        assertFailsWith<Exception> {
            OdinSystemSerializer.deserialize<TestPerson>(invalidJson)
        }
    }

    @Test
    fun testDeserialize_missingRequiredField_throwsException() {
        // Missing required field "lastName"
        val json = """{"firstName":"George","age":40}"""

        assertFailsWith<Exception> {
            OdinSystemSerializer.deserialize<TestPerson>(json)
        }
    }

    @Test
    fun testSerialize_encodeDefaults() {
        val person = TestPerson(
            FirstName = "Helen",
            LastName = "White",
            Age = 50
            // IsActive not specified, uses default value
        )

        val json = OdinSystemSerializer.serialize(person)

        // With encodeDefaults = true, default values should be included
        assertTrue(json.contains("\"isActive\""))
        assertTrue(json.contains("true"))
    }

    @Test
    fun testCamelCaseNamingStrategy_multipleUppercase() {
        @Serializable
        data class TestData(
            val XMLData: String,
            val HTTPSEnabled: Boolean
        )

        val data = TestData(XMLData = "test", HTTPSEnabled = true)
        val json = OdinSystemSerializer.serialize(data)

        // Should convert first character to lowercase
        assertTrue(json.contains("\"xMLData\""))
        assertTrue(json.contains("\"hTTPSEnabled\""))
    }

    @Test
    fun testSerialize_excludesNullValues() {
        val personWithNullContact = TestPersonWithAddress(
            PersonName = "John Doe",
            Address = TestAddress(
                StreetAddress = "123 Main St",
                City = "Anytown",
                PostalCode = "12345"
            ),
            ContactNumber = null
        )

        val json = OdinSystemSerializer.serialize(personWithNullContact)

        // Should convert to camelCase
        assertTrue(json.contains("\"personName\""))
        assertTrue(json.contains("\"address\""))
        assertTrue(json.contains("\"streetAddress\""))
        assertTrue(json.contains("\"city\""))
        assertTrue(json.contains("\"postalCode\""))
        
        // Should NOT contain null ContactNumber field
        assertTrue(!json.contains("\"contactNumber\""))
        assertTrue(!json.contains("null"))
        
        // Verify round-trip still works
        val deserialized = OdinSystemSerializer.deserialize<TestPersonWithAddress>(json)
        assertEquals(personWithNullContact.PersonName, deserialized.PersonName)
        assertEquals(personWithNullContact.Address.StreetAddress, deserialized.Address.StreetAddress)
        assertEquals(null, deserialized.ContactNumber)
    }
}
