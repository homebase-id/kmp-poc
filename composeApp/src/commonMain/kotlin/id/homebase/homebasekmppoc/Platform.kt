package id.homebase.homebasekmppoc

import kotlin.uuid.Uuid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun isAndroid(): Boolean

expect fun launchCustomTabs(url: String)

expect fun showMessage(title: String, message: String)

fun handleAuthCallback(code: String) {
    println("Auth code: $code")
    showMessage("Authentication Successful", "Received auth code: $code")
    // TODO: Process the auth code (validate, store token, etc.)
}

// URL encoder compatible with UTF-8 encoding
fun encodeUrl(value: String): String {
    val bytes = value.encodeToByteArray()
    val sb = StringBuilder()
    for (byte in bytes) {
        val b = byte.toInt() and 0xFF
        if ((b >= 48 && b <= 57) || (b >= 65 && b <= 90) || (b >= 97 && b <= 122) ||
            b == 45 || b == 46 || b == 95 || b == 126) { // unreserved characters
            sb.append(b.toChar())
        } else {
            sb.append('%')
            sb.append((b shr 4).toString(16).uppercase())
            sb.append((b and 15).toString(16).uppercase())
        }
    }
    return sb.toString()
}

// Generate a random UUID as byte array
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun generateUuidBytes(): ByteArray = Uuid.random().toByteArray()

// Generate a random UUID as byte array
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun generateUuidString(): String = Uuid.random().toString()
