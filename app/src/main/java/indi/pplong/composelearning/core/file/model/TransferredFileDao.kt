package indi.pplong.composelearning.core.file.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Description:
 * @author PPLong
 * @date 10/25/24 10:22 AM
 */
@Dao
interface TransferredFileDao {
    @Insert
    suspend fun insert(item: TransferredFileItem)

    @Query("SELECT * from transferred_file where transferType == 0")
    suspend fun getDownloadedItems(): List<TransferredFileItem>

    @Query("SELECT * from transferred_file where transferType == 1")
    suspend fun getUploadedItems(): List<TransferredFileItem>
}