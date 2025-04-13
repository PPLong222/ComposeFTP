package indi.pplong.composelearning.core.host.model

import indi.pplong.composelearning.ftp.FTPConfig

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:39 PM
 */
data class ServerItemInfo(
    val host: String = "",
    val password: String = "",
    val user: String = "",
    val port: Int = 0,
    val nickname: String = "",
    val lastConnectedTime: Long = 0,
    val isSFTP: Boolean = false,
    val connectedStatus: ServerConnectionStatus = ServerConnectionStatus.INITIAL
)

fun ServerItem.toItemInfo(): ServerItemInfo = ServerItemInfo(
    host, password, user, port, nickname, lastConnectedTime, isSFTP
)

fun ServerItemInfo.toItem(): ServerItem = ServerItem(
    host, password, user, port, nickname, lastConnectedTime, isSFTP
)

fun ServerItemInfo.toFTPConfig(): FTPConfig = FTPConfig(
    host, port, user, password, isSFTP
)

enum class ConnectivityTestState {
    INITIAL,
    TESTING,
    FAIL,
    SUCCESS
}