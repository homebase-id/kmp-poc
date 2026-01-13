package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.homebase.homebasekmppoc.lib.config.AUTO_CONNECTIONS_CIRCLE_ID
import id.homebase.homebasekmppoc.lib.config.AppConfig
import id.homebase.homebasekmppoc.lib.config.CONFIRMED_CONNECTIONS_CIRCLE_ID
import id.homebase.homebasekmppoc.lib.config.appPermissions
import id.homebase.homebasekmppoc.lib.config.circleDriveTargetRequest
import id.homebase.homebasekmppoc.lib.config.targetDriveAccessRequest
import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youauth.UsernameStorage
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.PlatformType
import id.homebase.homebasekmppoc.prototype.getPlatform
import id.homebase.homebasekmppoc.ui.extensions.cleanDomain
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen following strict MVI pattern.
 * - Single state via StateFlow
 * - Single entry point via onAction()
 * - One-off events via Channel
 */
class LoginViewModel(
        private val youAuthFlowManager: YouAuthFlowManager,
        private val usernameStorage: UsernameStorage = UsernameStorage(),
        private val odinClientFactory: OdinClientFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadUsernameFromStorage()
        checkExistingSession()
        observeAuthState()
    }

    private fun loadUsernameFromStorage() {
        val savedUsername = usernameStorage.loadUsername()
        if (savedUsername.isNotBlank()) {
            _uiState.update { it.copy(homebaseId = savedUsername) }
        }
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
        viewModelScope.launch {
            if (getPlatform().name != PlatformType.IOS) youAuthFlowManager.onAppResumed()
        }
    }

    private fun checkExistingSession() {
        if (youAuthFlowManager.restoreSession()) {
            _uiState.update { it.copy(isAuthenticated = true) }
            viewModelScope.launch { _uiEvent.send(LoginUiEvent.NavigateToHome) }
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
                val odinClient = odinClientFactory.createUnauthenticated(homebaseId)
                val httpClient = odinClient.createHttpClient()
                val response: HttpResponse = httpClient.get("health/ping")

                if (!response.status.isSuccess()) {
                    _uiState.update {
                        it.copy(
                                isLoading = false,
                                errorMessage =
                                        "Unable to ping $homebaseId - are you sure it's a Homebase ID?"
                        )
                    }
                    return@launch
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                            isLoading = false,
                            errorMessage =
                                    "Unable to contact $homebaseId - are you sure it's a Homebase ID? ${e.message}"
                    )
                }
                return@launch
            }

            // Start the YouAuth authorization flow
            try {
                youAuthFlowManager.authorize(
                        identity = homebaseId,
                        scope = viewModelScope,
                        appId = AppConfig.APP_ID,
                        appName = AppConfig.APP_NAME,
                        drives = targetDriveAccessRequest,
                        permissions = appPermissions,
                        circleDrives = circleDriveTargetRequest,
                        circles =
                                listOf(CONFIRMED_CONNECTIONS_CIRCLE_ID, AUTO_CONNECTIONS_CIRCLE_ID)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Login failed")
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            youAuthFlowManager.authState.collect { authState ->
                when (authState) {
                    is YouAuthState.Unauthenticated -> {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = false) }
                    }
                    is YouAuthState.Authenticating -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is YouAuthState.Authenticated -> {
                        usernameStorage.saveUsername(_uiState.value.homebaseId)
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                        _uiEvent.send(LoginUiEvent.NavigateToHome)
                    }
                    is YouAuthState.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = authState.message)
                        }
                    }
                }
            }
        }
    }
}
