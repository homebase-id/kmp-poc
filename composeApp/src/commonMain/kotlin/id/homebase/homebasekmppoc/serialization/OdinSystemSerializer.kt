package id.homebase.homebasekmppoc.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Custom camelCase naming strategy to match C# JsonNamingPolicy.CamelCase
 * Converts PascalCase property names to camelCase (first letter lowercase)
 */
@OptIn(ExperimentalSerializationApi::class)
object CamelCaseNamingStrategy : JsonNamingStrategy {
    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String {
        return serialName.replaceFirstChar { it.lowercase() }
    }
}

/**
 * Centralizes serialization functions using kotlinx.serialization
 */
object OdinSystemSerializer {
    /**
     * JSON configuration with camelCase naming strategy
     * Matches C# JsonNamingPolicy.CamelCase behavior:
     * - PropertyNameCaseInsensitive = true → ignoreUnknownKeys = true
     * - PropertyNamingPolicy = JsonNamingPolicy.CamelCase → custom CamelCaseNamingStrategy
     */
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        namingStrategy = CamelCaseNamingStrategy
    }

    /**
     * Serialize a value to JSON string
     */
    inline fun <reified T> serialize(value: T): String {
        return json.encodeToString(value)
    }

    /**
     * Deserialize a JSON string to type T
     */
    inline fun <reified T> deserialize(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }
}
