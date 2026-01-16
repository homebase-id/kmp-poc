package id.homebase.homebasekmppoc.prototype

expect suspend fun writeTextToTempFile(
    prefix: String,
    suffix: String,
    content: String
): String
