package id.homebase.homebasekmppoc.youauth

data class State(
    val identity: String,
    val privateKey: ByteArray?
)

// Global storage for states (not thread-safe in multiplatform context)
val globalStates: MutableMap<String, State> = mutableMapOf()