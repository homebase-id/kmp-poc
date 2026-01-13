package id.homebase.homebasekmppoc.prototype

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong()
        )
    }

actual suspend fun writeTextToTempFile(
    prefix: String,
    suffix: String,
    content: String
): String {
    val tempDir = NSTemporaryDirectory()
    val filePath = "$tempDir$prefix${NSUUID().UUIDString}$suffix"

    content
        .encodeToByteArray()
        .toNSData()
        .writeToFile(filePath, atomically = true)

    return filePath
}
