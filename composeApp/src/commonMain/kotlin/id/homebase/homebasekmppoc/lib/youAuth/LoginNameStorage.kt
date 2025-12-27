package id.homebase.homebasekmppoc.lib.youAuth

import id.homebase.homebasekmppoc.lib.storage.SecureStorage

/**
 * Handles login name storage operations using SecureStorage.
 * Provides functionality to load and save the last used Homebase ID
 * for convenient user experience between app sessions.
 */
class LoginNameStorage {

    companion object {
        private const val TAG = "LoginNameStorage"
    }

    /**
     * Load the last used Homebase ID from secure storage.
     * Returns null if no Homebase ID is found in storage.
     */
    fun loadLoginName(): String {
        var s = SecureStorage.get(YouAuthStorageKeys.LAST_HOMEBASE_ID)

        if (s.isNullOrEmpty())
            return ""
        else
            return s
    }
    
    /**
     * Save the Homebase ID to secure storage.
     */
    fun saveLoginName(homebaseId: String) {
        if (homebaseId.isNotBlank()) {
            SecureStorage.put(YouAuthStorageKeys.LAST_HOMEBASE_ID, homebaseId)
        }
    }
    
    /**
     * Delete the saved Homebase ID from secure storage.
     */
    fun deleteLoginName() {
        SecureStorage.remove(YouAuthStorageKeys.LAST_HOMEBASE_ID)
    }
}