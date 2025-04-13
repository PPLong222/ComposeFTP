package indi.pplong.composelearning.core.file.model

import indi.pplong.composelearning.core.util.MD5Utils
import org.apache.commons.net.ftp.FTPFile

/**
 * @author PPLong
 * @date 4/11/25 7:16 PM
 */
data class CommonFileInfo(
    val name: String,
    val isDir: Boolean,
    val path: String,
    val user: String,
    val mtime: Long,
    val size: Long
)

fun CommonFileInfo.getKey(host: String): String {
    return MD5Utils.digestMD5AsString("$host|$path|$name".toByteArray())
}

fun FTPFile.toCommonFileInfo(path: String): CommonFileInfo {
    return CommonFileInfo(
        name = name,
        isDir = isDirectory,
        path = path,
        user = user,
        mtime = timestamp.time.time,
        size = size
    )
}
