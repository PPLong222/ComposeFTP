package indi.pplong.composelearning.ftp.base

import indi.pplong.composelearning.core.file.model.CommonFileInfo

/**
 * @author PPLong
 * @date 4/11/25 5:59 PM
 */
interface IBaseFTPClient {
    suspend fun initClient(): Boolean

    suspend fun close()

    suspend fun list(): List<CommonFileInfo>

    suspend fun getCurrentPath(): String?

    suspend fun checkAndKeepAlive(): Boolean

    suspend fun changePath(pathName: String): Boolean
}