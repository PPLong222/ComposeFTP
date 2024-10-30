package indi.pplong.composelearning.core.host.repo

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import indi.pplong.composelearning.core.host.model.ServerItem

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:35 PM
 */
interface ServerItemRepository {
    @Insert
    suspend fun insert(item: ServerItem)

    @Delete
    suspend fun delete(item: ServerItem)

    @Query("SELECT * from server_items")
    suspend fun getAllItems(): List<ServerItem>
}