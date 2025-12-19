package id.homebase.homebasekmppoc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import id.homebase.homebasekmppoc.lib.core.ActivityProvider
import id.homebase.homebasekmppoc.lib.storage.SecureStorage
import id.homebase.homebasekmppoc.lib.storage.SharedPreferences
import id.homebase.homebasekmppoc.lib.youAuth.YouAuthFlowManager
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseDriverFactory
import id.homebase.homebasekmppoc.prototype.lib.database.DatabaseManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize ActivityProvider (replaces old lateinit var instance pattern)
        ActivityProvider.initialize(this)

        // Initialize storage (must be done before App() which may access storage)
        SecureStorage.initialize(applicationContext)
        SharedPreferences.initialize(applicationContext)
        FileKit.init(this)
        // Initialize database
        DatabaseManager.initialize(DatabaseDriverFactory(applicationContext))

        handleIntent(intent)

        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "youauth") {
            val callbackURL = data.toString()
            lifecycleScope.launch {
                YouAuthFlowManager.handleCallback(callbackURL)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update ActivityProvider reference on resume
        ActivityProvider.initialize(this)

        // Check if browser was closed without completing auth
        // This is called when user returns from Custom Tab without completing auth
        // We use a small delay to allow the callback to be processed first if it arrives
        lifecycleScope.launch {
            // Small delay to allow callback to be processed first
            kotlinx.coroutines.delay(300)

            // If no callback arrived and we're still authenticating, the user likely cancelled
            // This is handled inside YouAuthFlowManager.onAppResumed()
            // Note: We can't easily get YouAuthFlowManager from Koin here,
            // so the cancellation is handled in LoginViewModel instead
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
