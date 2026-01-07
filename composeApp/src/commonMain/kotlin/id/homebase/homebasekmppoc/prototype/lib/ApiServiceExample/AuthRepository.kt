package id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//

data class AuthState(
    val isLoggedIn: Boolean = false,
    val domain: String? = null
)

//

class AuthRepository(private val authManager: CredentialsManager) {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            syncState()
        }
    }

    private suspend fun syncState() {
        val active = authManager.getActiveCredentials()
        _state.value = AuthState(
            isLoggedIn = active != null,
            domain = active?.domain
        )
    }

    suspend fun login(apiCredentials: ApiCredentials) {
        authManager.setActiveCredentials(apiCredentials)
        syncState()
    }

    suspend fun logout() {
        authManager.removeActiveCredentials()
        syncState()
    }
}