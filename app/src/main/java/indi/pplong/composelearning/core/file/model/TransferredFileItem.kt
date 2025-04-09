package indi.pplong.composelearning.core.file.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
