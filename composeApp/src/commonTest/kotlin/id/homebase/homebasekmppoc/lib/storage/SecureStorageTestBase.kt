package id.homebase.homebasekmppoc.lib.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Common test logic for SecureStorage. Platform-specific test classes should inherit from this. */
abstract class SecureStorageTestBase {

    /** Called before each test to ensure clean state. Implementations should clear storage. */
    abstract fun setUp()

    @Test
    fun testPutAndGet() {
        setUp()

        val key = "test_key"
        val value = "test_value_12345"

        SecureStorage.put(key, value)
        val retrieved = SecureStorage.get(key)

        assertEquals(value, retrieved, "Retrieved value should match stored value")
    }

    @Test
    fun testGetNonExistent() {
        setUp()

        val result = SecureStorage.get("non_existent_key")

        assertNull(result, "Getting non-existent key should return null")
    }

    @Test
    fun testRemove() {
        setUp()

        val key = "key_to_remove"
        val value = "value_to_remove"

        SecureStorage.put(key, value)
        assertTrue(SecureStorage.contains(key), "Key should exist after put")

        SecureStorage.remove(key)

        assertFalse(SecureStorage.contains(key), "Key should not exist after remove")
        assertNull(SecureStorage.get(key), "Get should return null after remove")
    }

    @Test
    fun testContains() {
        setUp()

        val key = "contains_test_key"

        assertFalse(SecureStorage.contains(key), "Key should not exist initially")

        SecureStorage.put(key, "some_value")

        assertTrue(SecureStorage.contains(key), "Key should exist after put")
    }

    @Test
    fun testClear() {
        setUp()

        SecureStorage.put("key1", "value1")
        SecureStorage.put("key2", "value2")
        SecureStorage.put("key3", "value3")

        assertTrue(SecureStorage.contains("key1"))
        assertTrue(SecureStorage.contains("key2"))
        assertTrue(SecureStorage.contains("key3"))

        SecureStorage.clear()

        assertFalse(SecureStorage.contains("key1"), "key1 should not exist after clear")
        assertFalse(SecureStorage.contains("key2"), "key2 should not exist after clear")
        assertFalse(SecureStorage.contains("key3"), "key3 should not exist after clear")
    }

    @Test
    fun testOverwriteValue() {
        setUp()

        val key = "overwrite_key"
        val initialValue = "initial_value"
        val updatedValue = "updated_value"

        SecureStorage.put(key, initialValue)
        assertEquals(initialValue, SecureStorage.get(key))

        SecureStorage.put(key, updatedValue)
        assertEquals(updatedValue, SecureStorage.get(key), "Value should be overwritten")
    }

    @Test
    fun testSpecialCharacters() {
        setUp()

        val key = "special_chars_key"
        val value = "Value with special chars: !@#$%^&*(){}[]|\\:\";<>?,./ and unicode: 你好世界 🎉"

        SecureStorage.put(key, value)
        val retrieved = SecureStorage.get(key)

        assertEquals(value, retrieved, "Special characters should be preserved")
    }

    @Test
    fun testEmptyString() {
        setUp()

        val key = "empty_value_key"
        val value = ""

        SecureStorage.put(key, value)
        val retrieved = SecureStorage.get(key)

        assertEquals(value, retrieved, "Empty string should be stored and retrieved correctly")
    }

    @Test
    fun testLongValue() {
        setUp()

        val key = "long_value_key"
        val value = "A".repeat(10000) // 10KB of data

        SecureStorage.put(key, value)
        val retrieved = SecureStorage.get(key)

        assertEquals(value, retrieved, "Long value should be stored and retrieved correctly")
    }
}
