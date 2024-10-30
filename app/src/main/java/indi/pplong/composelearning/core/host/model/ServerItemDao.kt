package indi.pplong.composelearning.core.host.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 7:23 PM
 */
@Dao
interface ServerItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ServerItem)

    @Delete
    suspend fun delete(item: ServerItem)

    @Query("SELECT * from server_items")
    suspend fun getAllItems(): List<ServerItem>

}