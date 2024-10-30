package indi.pplong.composelearning.sys

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.host.repo.OfflineServerItemRepository
import indi.pplong.composelearning.core.host.repo.ServerItemRepository
import indi.pplong.composelearning.sys.database.CommonDatabase
import javax.inject.Singleton

/**
 * Description:
 * @author PPLong
 * @date 10/25/24 10:55 AM
 */
@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {
    @Provides
    @Singleton
    fun provideCommonDatabase(@ApplicationContext context: Context): CommonDatabase {
        return CommonDatabase.getDatabase(context)
    }

    @Provides
    fun provideServerItemRepository(database: CommonDatabase): ServerItemRepository =
        OfflineServerItemRepository(database.serverItemDao())

    @Provides
    @Singleton
    fun provideTransferredFileDao(database: CommonDatabase): TransferredFileDao =
        database.transferredFileDao()
}