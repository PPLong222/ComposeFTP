package indi.pplong.composelearning.core.host.repo

import indi.pplong.composelearning.core.host.model.ServerItem
import indi.pplong.composelearning.core.host.model.ServerItemDao
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:35 PM
 */
class OfflineServerItemRepository @Inject constructor(private val serverItemDao: ServerItemDao) :
    ServerItemRepository {
    override suspend fun insert(item: ServerItem) = serverItemDao.insert(item)

    override suspend fun delete(item: ServerItem) = serverItemDao.delete(item)

    override suspend fun getAllItems(): List<ServerItem> = serverItemDao.getAllItems()
}