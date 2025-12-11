package id.homebase.homebasekmppoc.lib.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Android instrumented tests for SharedPreferences. */
@RunWith(AndroidJUnit4::class)
class SharedPreferencesTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SharedPreferences.initialize(context)
        SharedPreferences.clear()
    }

    // String tests
    @Test
    fun testPutAndGetString() {
        val key = "string_key"
        val value = "test_value_12345"

        SharedPreferences.putString(key, value)
        val retrieved = SharedPreferences.getString(key)

        assertEquals(value, retrieved, "Retrieved string value should match stored value")
    }

    @Test
    fun testGetStringNonExistent() {
        val result = SharedPreferences.getString("non_existent_key")
        assertNull(result, "Getting non-existent key should return null")
    }

    @Test
    fun testEmptyString() {
        val key = "empty_string_key"
        val value = ""

        SharedPreferences.putString(key, value)
        val retrieved = SharedPreferences.getString(key)

        assertEquals(value, retrieved, "Empty string should be stored and retrieved correctly")
    }

    @Test
    fun testSpecialCharacters() {
        val key = "special_chars_key"
        val value = "Value with special chars: !@#\$%^&*(){}[]|\\:\";<>?,./ and unicode: 你好世界 🎉"

        SharedPreferences.putString(key, value)
        val retrieved = SharedPreferences.getString(key)

        assertEquals(value, retrieved, "Special characters should be preserved")
    }

    // Int tests
    @Test
    fun testPutAndGetInt() {
        val key = "int_key"
        val value = 42

        SharedPreferences.putInt(key, value)
        val retrieved = SharedPreferences.getInt(key, 0)

        assertEquals(value, retrieved, "Retrieved int value should match stored value")
    }

    @Test
    fun testGetIntWithDefault() {
        val defaultValue = 99
        val result = SharedPreferences.getInt("non_existent_int", defaultValue)
        assertEquals(defaultValue, result, "Should return default value for non-existent key")
    }

    @Test
    fun testIntNegativeValue() {
        val key = "negative_int_key"
        val value = -12345

        SharedPreferences.putInt(key, value)
        val retrieved = SharedPreferences.getInt(key, 0)

        assertEquals(value, retrieved, "Negative int should be stored correctly")
    }

    @Test
    fun testIntMinMax() {
        SharedPreferences.putInt("min_int", Int.MIN_VALUE)
        SharedPreferences.putInt("max_int", Int.MAX_VALUE)

        assertEquals(Int.MIN_VALUE, SharedPreferences.getInt("min_int", 0))
        assertEquals(Int.MAX_VALUE, SharedPreferences.getInt("max_int", 0))
    }

    // Long tests
    @Test
    fun testPutAndGetLong() {
        val key = "long_key"
        val value = 9876543210L

        SharedPreferences.putLong(key, value)
        val retrieved = SharedPreferences.getLong(key, 0L)

        assertEquals(value, retrieved, "Retrieved long value should match stored value")
    }

    @Test
    fun testGetLongWithDefault() {
        val defaultValue = 999L
        val result = SharedPreferences.getLong("non_existent_long", defaultValue)
        assertEquals(defaultValue, result, "Should return default value for non-existent key")
    }

    @Test
    fun testLongMinMax() {
        SharedPreferences.putLong("min_long", Long.MIN_VALUE)
        SharedPreferences.putLong("max_long", Long.MAX_VALUE)

        assertEquals(Long.MIN_VALUE, SharedPreferences.getLong("min_long", 0L))
        assertEquals(Long.MAX_VALUE, SharedPreferences.getLong("max_long", 0L))
    }

    // Boolean tests
    @Test
    fun testPutAndGetBoolean() {
        SharedPreferences.putBoolean("bool_true", true)
        SharedPreferences.putBoolean("bool_false", false)

        assertTrue(SharedPreferences.getBoolean("bool_true", false))
        assertFalse(SharedPreferences.getBoolean("bool_false", true))
    }

    @Test
    fun testGetBooleanWithDefault() {
        assertTrue(SharedPreferences.getBoolean("non_existent_bool", true))
        assertFalse(SharedPreferences.getBoolean("non_existent_bool", false))
    }

    // Float tests
    @Test
    fun testPutAndGetFloat() {
        val key = "float_key"
        val value = 3.14159f

        SharedPreferences.putFloat(key, value)
        val retrieved = SharedPreferences.getFloat(key, 0f)

        assertEquals(value, retrieved, 0.0001f, "Retrieved float value should match stored value")
    }

    @Test
    fun testGetFloatWithDefault() {
        val defaultValue = 1.5f
        val result = SharedPreferences.getFloat("non_existent_float", defaultValue)
        assertEquals(
                defaultValue,
                result,
                0.0001f,
                "Should return default value for non-existent key"
        )
    }

    // Double tests
    @Test
    fun testPutAndGetDouble() {
        val key = "double_key"
        val value = 3.141592653589793

        SharedPreferences.putDouble(key, value)
        val retrieved = SharedPreferences.getDouble(key, 0.0)

        assertEquals(
                value,
                retrieved,
                0.0000001,
                "Retrieved double value should match stored value"
        )
    }

    @Test
    fun testGetDoubleWithDefault() {
        val defaultValue = 2.718281828
        val result = SharedPreferences.getDouble("non_existent_double", defaultValue)
        assertEquals(
                defaultValue,
                result,
                0.0000001,
                "Should return default value for non-existent key"
        )
    }

    // Common operations tests
    @Test
    fun testRemove() {
        val key = "key_to_remove"
        SharedPreferences.putString(key, "value")
        assertTrue(SharedPreferences.contains(key), "Key should exist after put")

        SharedPreferences.remove(key)

        assertFalse(SharedPreferences.contains(key), "Key should not exist after remove")
        assertNull(SharedPreferences.getString(key), "Get should return null after remove")
    }

    @Test
    fun testContains() {
        val key = "contains_test_key"

        assertFalse(SharedPreferences.contains(key), "Key should not exist initially")

        SharedPreferences.putString(key, "some_value")

        assertTrue(SharedPreferences.contains(key), "Key should exist after put")
    }

    @Test
    fun testClear() {
        SharedPreferences.putString("str_key", "value")
        SharedPreferences.putInt("int_key", 42)
        SharedPreferences.putBoolean("bool_key", true)

        assertTrue(SharedPreferences.contains("str_key"))
        assertTrue(SharedPreferences.contains("int_key"))
        assertTrue(SharedPreferences.contains("bool_key"))

        SharedPreferences.clear()

        assertFalse(SharedPreferences.contains("str_key"), "str_key should not exist after clear")
        assertFalse(SharedPreferences.contains("int_key"), "int_key should not exist after clear")
        assertFalse(SharedPreferences.contains("bool_key"), "bool_key should not exist after clear")
    }

    @Test
    fun testOverwriteValue() {
        val key = "overwrite_key"
        val initialValue = "initial"
        val updatedValue = "updated"

        SharedPreferences.putString(key, initialValue)
        assertEquals(initialValue, SharedPreferences.getString(key))

        SharedPreferences.putString(key, updatedValue)
        assertEquals(updatedValue, SharedPreferences.getString(key), "Value should be overwritten")
    }
}
