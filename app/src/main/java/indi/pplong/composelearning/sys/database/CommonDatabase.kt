package indi.pplong.composelearning.sys.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.host.model.ServerItem
import indi.pplong.composelearning.core.host.model.ServerItemDao

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:27 PM
 */
@Database(
    entities = [ServerItem::class, TransferredFileItem::class],
    version = 1,
    exportSchema = false
)
abstract class CommonDatabase : RoomDatabase() {
    abstract fun serverItemDao(): ServerItemDao

    abstract fun transferredFileDao(): TransferredFileDao

    companion object {
        private var Instance: CommonDatabase? = null
        private const val DATABASE_NAME = "common_db"
        fun getDatabase(context: Context): CommonDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, CommonDatabase::class.java, DATABASE_NAME)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}