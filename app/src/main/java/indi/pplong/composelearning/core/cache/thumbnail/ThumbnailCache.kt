package indi.pplong.composelearning.core.cache.thumbnail

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Description:
 * @author PPLong
 * @date 11/3/24 3:54 PM
 */
@Entity(tableName = "thumb_nail_info")
data class ThumbnailCache(
    @PrimaryKey
    val id: String,
    val uri: String?
)
