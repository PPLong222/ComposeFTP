package indi.pplong.composelearning.core.cache.thumbnail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Description:
 * @author PPLong
 * @date 11/3/24 4:28 PM
 */
@Dao
interface ThumbnailCacheDao {
    @Insert
    suspend fun insert(info: ThumbnailCache)

    @Query("SELECT * from thumb_nail_info")
    suspend fun getThumbNailCacheList(): List<ThumbnailCache>

    @Query("DELETE from thumb_nail_info")
    suspend fun delete()
}