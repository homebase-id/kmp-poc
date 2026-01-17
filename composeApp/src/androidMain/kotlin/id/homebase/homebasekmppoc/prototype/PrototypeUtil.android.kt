package id.homebase.homebasekmppoc.prototype

import java.io.File

actual suspend fun writeTextToTempFile(
    prefix: String,
    suffix: String,
    content: String
): String {
    val file = File.createTempFile(prefix, suffix)
    file.writeText(content)
    return file.absolutePath
}
