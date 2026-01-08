package id.homebase.homebasekmppoc.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import id.homebase.homebasekmppoc.lib.youauth.DrivePermissionType
import id.homebase.homebasekmppoc.lib.youauth.OdinClientFactory
import id.homebase.homebasekmppoc.lib.youauth.TargetDriveAccessRequest
import id.homebase.homebasekmppoc.lib.youauth.UsernameStorage
import id.homebase.homebasekmppoc.lib.youauth.YouAuthFlowManager
import id.homebase.homebasekmppoc.lib.youauth.YouAuthState
import id.homebase.homebasekmppoc.prototype.lib.ApiServiceExample.ApiExampleService
import id.homebase.homebasekmppoc.prototype.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.prototype.lib.http.CreateHttpClientOptions
import id.homebase.homebasekmppoc.prototype.lib.http.OdinClient
import id.homebase.homebasekmppoc.prototype.lib.http.createHttpClient
import id.homebase.homebasekmppoc.ui.extensions.cleanDomain
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
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


val publicPostsDriveId = Uuid.parse("e8475dc46cb4b6651c2d0dbd0f3aad5f")

val channelDriveType = Uuid.parse("8f448716-e34c-edf9-0141-45e043ca6612")

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
        ),
        TargetDriveAccessRequest(
            alias = publicPostsDriveId.toString(),
            type = channelDriveType.toString(),
            name = "Public Posts Drive",
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
    private val usernameStorage: UsernameStorage = UsernameStorage(),
    private val odinClientFactory: OdinClientFactory,
    private val apiService: ApiExampleService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadUsernameFromStorage()
        observeAuthState()

        // SEB:NOTE Bishwa: checkExistingSession became suspend fun, so we need to launch a coroutine
        viewModelScope.launch {
            try {
                checkExistingSession()
            } catch (e: Exception) {
                Logger.e("LoginViewModel", e) { "Error checking existing session: ${e.message}" }
            }
        }
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
        viewModelScope.launch { youAuthFlowManager.onAppResumed() }
    }

    private suspend fun checkExistingSession() {
        if (youAuthFlowManager.restoreSession()) {
            _uiState.update { it.copy(isAuthenticated = true) }
            _uiEvent.send(LoginUiEvent.NavigateToHome)
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

                val success = apiService.Ping(homebaseId);
                if (!success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to ping $homebaseId - are you sure it's a Homebase ID?"
                        )
                    }
                    return@launch
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to contact $homebaseId - are you sure it's a Homebase ID? ${e.message}"
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
                    drives = targetDriveAccessRequest
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
