package indi.pplong.composelearning.core.file.model

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 4:30 PM
 */
data class ActiveServer(
    val serverHost: String = "",
    val serverNickname: String = "",
    val hasUploadNode: Boolean = false,
    val hasDownloadNode: Boolean = false
)
