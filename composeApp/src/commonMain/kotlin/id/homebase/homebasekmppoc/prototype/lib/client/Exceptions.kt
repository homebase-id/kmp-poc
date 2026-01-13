package id.homebase.homebasekmppoc.prototype.lib.client

data class ProblemDetails(
    val status: Int? = null,
    val title: String? = null,
    val type: String? = null,
    val extensions: Map<String, Any?> = emptyMap()
)

@Suppress("UNCHECKED_CAST")
fun ProblemDetails.errorCode(): String? =
    extensions["errorCode"] as? String

@Suppress("UNCHECKED_CAST")
fun ProblemDetails.correlationId(): String? =
    extensions["correlationId"] as? String


class ClientException(
    status: Int,
    val errorCode: String?,
    message: String,
    correlationId: String?,
    problem: ProblemDetails
) : OdinApiException(status, message, correlationId, problem)


class UnauthorizedException :
    OdinApiException(401, "Unauthorized")

class ForbiddenException :
    OdinApiException(403, "Forbidden")

class NotFoundException :
    OdinApiException(404, "Not found")

class ServerException(
    status: Int,
    correlationId: String?,
    problem: ProblemDetails?
) : OdinApiException(status, "Server error (status=$status)", correlationId, problem)

