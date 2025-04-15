package indi.pplong.composelearning.core.host.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 7:19 PM
 */
@Entity(
    tableName = "server_items",
    indices = [
        Index(value = ["host", "user", "isSFTP"], unique = true),
        Index(value = ["nickname"], unique = true)
    ]
)
data class ServerItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val host: String,
    val password: String,
    val user: String,
    val port: Int,
    val nickname: String,
    val lastConnectedTime: Long,
    val isSFTP: Boolean,
    val downloadDir: String?
)