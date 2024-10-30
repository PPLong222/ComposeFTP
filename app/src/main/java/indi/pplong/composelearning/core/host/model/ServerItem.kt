package indi.pplong.composelearning.core.host.model

import androidx.room.Entity

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 7:19 PM
 */
@Entity(
    tableName = "server_items",
    primaryKeys = ["host", "user"]
)
data class ServerItem(
    val host: String,
    val password: String,
    val user: String,
    val port: Int,
    val nickname: String,
    val lastConnectedTime: Long,
)