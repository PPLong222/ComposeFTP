package indi.pplong.composelearning.ftp

/**
 * Description:
 * @author PPLong
 * @date 4/11/25 4:55 PM
 */
data class FTPConfig(
    val host: String,
    val port: Int?,
    val username: String,
    val password: String,
    val isSFTP: Boolean = false
)
