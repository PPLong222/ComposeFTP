package indi.pplong.composelearning.core.file.model

import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.util.MD5Utils
import org.apache.commons.net.ftp.FTPFile
import java.time.ZoneId

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 1:44 PM
 */
data class FileItemInfo(
    val name: String = "aaasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasd",
    val isDir: Boolean = true,
    val pathPrefix: String = "",
    val user: String = "",
    val timeStamp: Long = 0,
    val size: Long = 0,
    val timeStampZoneId: ZoneId = ZoneId.systemDefault(),
    val transferStatus: TransferStatus = TransferStatus.Initial,
    val localImageUri: String = "",
    val md5: String = "",
    val fullPath: String = "$pathPrefix/$name",
    val isSelected: Boolean = false
)

fun FileItemInfo.getKey(hostKey: Long): String {
    return MD5Utils.digestMD5AsString("${hostKey.toString()}|$pathPrefix|$name".toByteArray())
}


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
        localImageUri = localUri
    )

fun CommonFileInfo.toFileItemInfo(prefix: String, md5: String, localUri: String): FileItemInfo =
    FileItemInfo(
        name,
        isDir,
        if (prefix == "/") "" else prefix,
        user,
        mtime,
        size,
        ZoneId.systemDefault(),
        md5 = md5,
        localImageUri = localUri
    )