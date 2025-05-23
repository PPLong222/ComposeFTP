package indi.pplong.composelearning.core.host.model

import indi.pplong.composelearning.ftp.FTPConfig

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:39 PM
 */
data class ServerItemInfo(
    val id: Long = 0L,
    val host: String = "",
    val password: String = "",
    val user: String = "",
    val port: Int = 0,
    val nickname: String = "",
    val lastConnectedTime: Long = 0,
    val isSFTP: Boolean = false,
    val connectedStatus: ServerConnectionStatus = ServerConnectionStatus.INITIAL,
    val downloadDir: String? = null
)

fun ServerItem.toItemInfo(): ServerItemInfo = ServerItemInfo(
    id, host, password, user, port, nickname, lastConnectedTime, isSFTP, downloadDir = downloadDir
)

fun ServerItemInfo.toItem(): ServerItem = ServerItem(
    id, host, password, user, port, nickname, lastConnectedTime, isSFTP, downloadDir
)

fun ServerItemInfo.toFTPConfig(): FTPConfig = FTPConfig(
    id, host, port, user, password, isSFTP, downloadDir
)

enum class ConnectivityTestState {
    INITIAL,
    TESTING,
    FAIL,
    SUCCESS
}