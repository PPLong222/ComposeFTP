package indi.pplong.composelearning.ftp.base

import android.content.Context
import indi.pplong.composelearning.ftp.PoolContext

/**
 * @author PPLong
 * @date 4/11/25 7:46 PM
 */
interface ICoreFTPClient : IBaseFTPClient {

    suspend fun renameFile(originalName: String, newName: String): Boolean

    suspend fun createDirectory(dirName: String): Boolean

    suspend fun deleteDirectory(pathName: List<String>): Boolean

    suspend fun deleteFile(pathName: List<String>): Boolean

    fun createThumbnailClient(): IThumbnailFTPClient

    fun createTransferFTPClient(poolContext: PoolContext, context: Context): ITransferFTPClient
}