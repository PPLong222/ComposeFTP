package indi.pplong.composelearning.core.file.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import indi.pplong.composelearning.core.util.MD5Utils

/**
 * Description:
 * @author PPLong
 * @date 9/27/24 11:17 AM
 */
@Entity("transferred_file")
data class TransferredFileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteName: String = "",
    val remotePathPrefix: String = "",
    val timeMills: Long = 0,
    val timeZoneId: String = "",
    val size: Long = 0,
    val isComplete: Boolean = false,
    val serverKey: Long = 0L,
    /**
     * 0: Download
     * 1: Upload
     */
    val transferType: Int = 0,
    val localUri: String = "",
    val localImageUri: String = ""
)

fun TransferredFileItem.keyEquals(other: TransferredFileItem): Boolean {
    return remoteName == other.remoteName
            && remotePathPrefix == other.remotePathPrefix
            && size == other.size
            && serverKey == other.serverKey
            && transferType == other.transferType
}

fun TransferredFileItem.getKey(hostKey: Long): String {
    return MD5Utils.digestMD5AsString("$hostKey|$remotePathPrefix|$remoteName".toByteArray())
}

fun CommonFileInfo.toTransferredFileItem(
    serverKey: Long,
    transferType: Int,
    bytesOffset: Long
): TransferredFileItem {
    return TransferredFileItem(
        serverKey = serverKey,
        remoteName = name,
        remotePathPrefix = path,
        timeMills = mtime,
        size = size,
        transferType = transferType,
        localUri = localUri,
        localImageUri = localImageUri
    )
}