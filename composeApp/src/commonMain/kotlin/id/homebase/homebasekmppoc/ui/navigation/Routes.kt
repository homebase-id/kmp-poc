package id.homebase.homebasekmppoc.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Navigation 3. Using @Serializable for compile-time safety and
 * type-safe argument passing.
 */
@Serializable
sealed class Route {
    @Serializable data object Splash : Route()

    @Serializable data object Login : Route()

    @Serializable data object Home : Route()

    @Serializable data object DriveFetch : Route()

    @Serializable data object Database : Route()

    @Serializable data object WebSocket : Route()

    @Serializable data object Video : Route()

    @Serializable data object CdnTest : Route()

    @Serializable data object DriveUpload : Route()

    @Serializable data class FileDetail(val driveId: String, val fileId: String) : Route()

    @Serializable data object FFmpegTest : Route()

    @Serializable data object ChatList : Route()

    @Serializable data class ChatMessageDetail(val driveId: String, val fileId: String) : Route()

    @Serializable data class ChatMessages(val conversationId: String) : Route()
}

/** Deep link configuration */
object DeepLinks {
    const val YOUAUTH_SCHEME = "youauth"
    const val YOUAUTH_CALLBACK = "youauth://callback"
}
