package id.homebase.homebasekmppoc.lib.authentication

import kotlinx.serialization.Serializable

@Serializable
class AuthenticationResponse (val sharedSecret: String)
