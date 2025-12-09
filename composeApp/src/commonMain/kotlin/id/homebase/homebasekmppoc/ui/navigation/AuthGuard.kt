package id.homebase.homebasekmppoc.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.homebase.homebasekmppoc.prototype.lib.authentication.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Wrapper composable that protects routes requiring authentication. Redirects to login if user is
 * not authenticated.
 *
 * @param authState StateFlow of the current authentication state
 * @param onUnauthenticated Callback invoked when user is not authenticated
 * @param content The protected content to display when authenticated
 */
@Composable
fun AuthenticatedRoute(
        authState: StateFlow<AuthState>,
        onUnauthenticated: () -> Unit,
        content: @Composable () -> Unit
) {
    val state by authState.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            is AuthState.Unauthenticated -> onUnauthenticated()
            is AuthState.Error -> onUnauthenticated()
            else -> {
                /* Stay on current route */
            }
        }
    }

    when (state) {
        is AuthState.Authenticated -> content()
        is AuthState.Authenticating -> LoadingScreen()
        is AuthState.Unauthenticated -> {
            /* Will redirect via LaunchedEffect */
        }
        is AuthState.Error -> {
            /* Will redirect via LaunchedEffect */
        }
    }
}

/** Loading screen shown during authentication */
@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
