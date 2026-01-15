package id.homebase.homebasekmppoc.prototype.lib.base

sealed class OdinApiException(
    val status: Int,
    message: String,
    val correlationId: String? = null,
    val problem: ProblemDetails? = null
) : RuntimeException(message)
