package indi.pplong.composelearning.core.file.model

import indi.pplong.composelearning.core.cache.TransferStatus
import org.apache.commons.net.ftp.FTPFile
import java.time.ZoneId

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 1:44 PM
 */
data class FileItemInfo(
    val name: String = "aaasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasd",
    val isDir: Boolean = false,
    val pathPrefix: String = "",
    val user: String = "",
    val timeStamp: Long = 0,
    val size: Long = 0,
    val timeStampZoneId: ZoneId = ZoneId.systemDefault(),
    val transferStatus: TransferStatus = TransferStatus.Initial,
    val localUri: String = "",
    val md5: String = "",
    val fullPath: String = "$pathPrefix/$name"
)

fun FTPFile.toFileItemInfo(prefix: String, md5: String, localUri: String): FileItemInfo =
    FileItemInfo(
        name,
        isDirectory,
        if (prefix == "/") "" else prefix,
        user,
        timestamp.time.time,
        size,
        timestamp.timeZone.toZoneId(),
        md5 = md5,
        localUri = localUri
    )