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
    val id: Int = 0,
    val remoteName: String = "",
    val remotePathPrefix: String = "",
    val timeMills: Long = 0,
    val timeZoneId: String = "",
    val size: Long = 0,
    val serverHost: String = "",
    /**
     * 0: Download
     * 1: Upload
     */
    val transferType: Int = 0,
    val localUri: String = "",
    val localImageUri: String = ""
)

fun TransferredFileItem.getKey(host: String): String {
    return MD5Utils.digestMD5AsString("$host|$remotePathPrefix|$remoteName".toByteArray())
}

fun FileItemInfo.toTransferredFileItem(
    host: String,
    transferType: Int,
    localUri: String
): TransferredFileItem {
    return TransferredFileItem(
        remoteName = name,
        remotePathPrefix = pathPrefix,
        timeMills = timeStamp,
        size = size,
        timeZoneId = timeStampZoneId.id,
        transferType = transferType,
        localUri = localUri,
        localImageUri = localImageUri,
        serverHost = host
    )
}