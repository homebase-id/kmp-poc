package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.youAuth.DrivePermissionType
import id.homebase.homebasekmppoc.lib.youAuth.LoginNameStorage
import id.homebase.homebasekmppoc.lib.youAuth.TargetDriveAccessRequest
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.http.createHttpClient
import id.homebase.homebasekmppoc.ui.extensions.cleanDomain
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private object AppConfig {
    const val APP_ID = "0cecc6fe033e48b19ee6a4f60318be02"
    const val APP_NAME = "Homebase - KMP POC"
}

val feedTargetDrive: TargetDrive =
        TargetDrive(
                alias = Uuid.parse("4db49422ebad02e99ab96e9c477d1e08"),
                type = Uuid.parse("a3227ffba87608beeb24fee9b70d92a6")
        )

var targetDriveAccessRequest: List<TargetDriveAccessRequest> =
        listOf(
                TargetDriveAccessRequest(
                        alias = feedTargetDrive.alias.toString(),
                        type = feedTargetDrive.type.toString(),
                        name = "Feed Drive",
                        description = " ",
                        permissions =
                                listOf(
                                        DrivePermissionType.Read,
                                        DrivePermissionType.Write,
                                )
                )
        )

/**
 * ViewModel for Login screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class LoginViewModel(
    private val youAuthFlowManager: YouAuthFlowManager,
    private val loginNameStorage: LoginNameStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        Logger.d("LoginViewModel", null, "LoginViewModel init - starting")
        loadSavedLoginName()
        checkExistingSession()
        observeAuthState()
        Logger.d("LoginViewModel", null, "LoginViewModel init - completed")
    }

    /** Load the last saved Homebase ID on initialization. */
    private fun loadSavedLoginName() {
        val savedHomebaseId = loginNameStorage.loadLoginName()
        _uiState.update { it.copy(homebaseId = savedHomebaseId) }
    }

    /** Single entry point for all UI actions. */
    fun onAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.HomebaseIdChanged -> {
                _uiState.update {
                    it.copy(homebaseId = action.value.cleanDomain(), errorMessage = null)
                }
            }
            is LoginUiAction.LoginClicked -> {
                performLogin()
            }
            is LoginUiAction.RetryClicked -> {
                performLogin()
            }
            is LoginUiAction.AppResumed -> {
                handleAppResumed()
            }
        }
    }

    private fun handleAppResumed() {
        Logger.d("LoginViewModel", null, "handleAppResumed called")
        viewModelScope.launch { 
            Logger.d("LoginViewModel", null, "Calling onAppResumed at ${System.currentTimeMillis()}")
            youAuthFlowManager.onAppResumed() 
        }
    }

    private fun checkExistingSession() {
        try {
            Logger.d("LoginViewModel", null, "checkExistingSession - starting")
            val sessionRestored = youAuthFlowManager.restoreSession()
            Logger.d("LoginViewModel", null, "checkExistingSession - restoreSession returned: $sessionRestored")
            if (sessionRestored) {
                _uiState.update { it.copy(isAuthenticated = true) }
                viewModelScope.launch { _uiEvent.send(LoginUiEvent.NavigateToHome) }
            }
        } catch (e: Exception) {
            Logger.e("LoginViewModel", e, "Error checking existing session")
            _uiState.update { it.copy(errorMessage = "Error checking existing session: ${e.message}") }
        }
    }

private fun performLogin() {
        val homebaseId = _uiState.value.homebaseId.cleanDomain(false)
        if (homebaseId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid Homebase ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Verify the identity is reachable before starting auth flow
            try {
                val httpClient = createHttpClient()
                val pingUrl = "https://$homebaseId/api/v1/ping"
                val response: HttpResponse = httpClient.head(pingUrl)

                if (!response.status.isSuccess()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Unable to ping $homebaseId - are you sure it's a Homebase ID?") }
                    return@launch
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Unable to contact $homebaseId - are you sure it's a Homebase ID? ${e.message}") }
                return@launch
            }

            // Start the YouAuth authorization flow
            try {
                Logger.d("LoginViewModel", null, "Starting authorization for: $homebaseId")
                youAuthFlowManager.authorize(
                        identity = homebaseId,
                        scope = viewModelScope,
                        appId = AppConfig.APP_ID,
                        appName = AppConfig.APP_NAME,
                        drives = targetDriveAccessRequest
                )
                Logger.d("LoginViewModel", null, "Authorization method completed")
            } catch (e: Exception) {
                Logger.e("LoginViewModel", e, "Error starting authorization")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to start login: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            youAuthFlowManager.authState.collect { authState ->
                Logger.d("LoginViewModel", null, "Auth state changed to: ${authState::class.simpleName}, message: ${if (authState is YouAuthState.Error) authState.message else "N/A"}")
                when (authState) {
                    is YouAuthState.Unauthenticated -> {
                        Logger.d("LoginViewModel", null, "Processing Unauthenticated state, current error: ${_uiState.value.errorMessage}")
                        // Only clear error if it was from a successful logout, not from an error
                        val currentError = _uiState.value.errorMessage
                        _uiState.update { 
                            it.copy(isLoading = false, isAuthenticated = false, 
                                errorMessage = if (currentError?.contains("logout") == true) null else currentError)
                        }
                        Logger.d("LoginViewModel", null, "Updated state to Unauthenticated, error: ${_uiState.value.errorMessage}")
                    }
                    is YouAuthState.Authenticating -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is YouAuthState.Authenticated -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, errorMessage = null) }
                        
                        // Save the successful homebaseId for next time
                        val currentHomebaseId = _uiState.value.homebaseId.cleanDomain(false)
                        if (currentHomebaseId.isNotBlank()) {
                            loginNameStorage.saveLoginName(currentHomebaseId)
                        }
                        
                        _uiEvent.send(LoginUiEvent.NavigateToHome)
                    }
                    is YouAuthState.Error -> {
                        Logger.w("LoginViewModel", null, "Authentication error: ${authState.message}")
                        _uiState.update {
                            it.copy(isLoading = false, isAuthenticated = false, errorMessage = authState.message ?: "Authentication failed")
                        }
                    }
                }
            }
        }
    }
}
