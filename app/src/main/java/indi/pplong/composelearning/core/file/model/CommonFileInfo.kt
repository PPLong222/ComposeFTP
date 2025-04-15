package indi.pplong.composelearning.core.file.model

import android.os.Parcelable
import indi.pplong.composelearning.core.util.MD5Utils
import kotlinx.parcelize.Parcelize
import org.apache.commons.net.ftp.FTPFile

/**
 * @author PPLong
 * @date 4/11/25 7:16 PM
 */
@Parcelize
data class CommonFileInfo(
    val name: String,
    val isDir: Boolean,
    val path: String,
    val user: String = "",
    val mtime: Long = 0L,
    val size: Long,
    val localUri: String = "",
    val localImageUri: String = ""
) : Parcelable

fun CommonFileInfo.getKey(hostKey: Long): String {
    return MD5Utils.digestMD5AsString("$hostKey|$path|$name".toByteArray())
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