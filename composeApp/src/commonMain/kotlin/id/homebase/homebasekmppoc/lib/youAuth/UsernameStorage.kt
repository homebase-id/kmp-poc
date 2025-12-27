package id.homebase.homebasekmppoc.lib.youAuth

import id.homebase.homebasekmppoc.lib.storage.SecureStorage

/**
 * Handles username storage operations using SecureStorage.
 * Provides functionality to load and save the last used username
 * for convenient user experience between app sessions.
 */
class UsernameStorage {

    companion object {
        private const val TAG = "UsernameStorage"
    }

    /**
     * Load the last used username from secure storage.
     * Returns empty string if no username is found in storage.
     */
    fun loadUsername(): String {
        val s = SecureStorage.get(YouAuthStorageKeys.USERNAME)

        if (s.isNullOrEmpty())
            return ""
        else
            return s
    }

    /**
     * Save the username to secure storage.
     */
    fun saveUsername(username: String) {
        if (username.isNotBlank()) {
            SecureStorage.put(YouAuthStorageKeys.USERNAME, username)
        }
    }

    /**
     * Delete the saved username from secure storage.
     */
    fun deleteUsername() {
        SecureStorage.remove(YouAuthStorageKeys.USERNAME)
    }
}