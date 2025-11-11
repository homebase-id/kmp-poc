package id.homebase.homebasekmppoc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun isAndroid(): Boolean

expect fun launchCustomTabs(url: String)

expect fun showAuthDialog(title: String, message: String)

fun handleAuthCallback(code: String) {
    println("Auth code: $code")
    showAuthDialog("Authentication Successful", "Received auth code: $code")
    // TODO: Process the auth code (validate, store token, etc.)
}