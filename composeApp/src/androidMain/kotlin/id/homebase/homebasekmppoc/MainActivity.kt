package id.homebase.homebasekmppoc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.homebase.homebasekmppoc.youauth.handleAuthorizeCallback

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var instance: MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "youauth") {
            // Convert Uri to full URL string including scheme, host, path, and query params
            val callbackURL = data.toString()
            //showMessage("Auth Callback", "Received URL: $callbackURL")
            handleAuthorizeCallback(callbackURL)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}