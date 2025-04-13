package indi.pplong.composelearning.ftp.sftp

import android.content.Context
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.PoolContext
import indi.pplong.composelearning.ftp.base.ICoreFTPClient
import indi.pplong.composelearning.ftp.base.IThumbnailFTPClient
import indi.pplong.composelearning.ftp.base.ITransferFTPClient

/**
 * @author PPLong
 * @date 4/13/25 11:18 AM
 */
class SFTPCoreClient(config: FTPConfig, val context: Context) : SFTPBaseClient(config),
    ICoreFTPClient {
    override suspend fun renameFile(
        originalName: String,
        newName: String
    ): Boolean {
        return try {
            sftp.rename(originalName, newName)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun createDirectory(dirName: String): Boolean {
        return try {
            sftp.mkdir(dirName)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteDirectory(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { path ->
                sftp.rmdir(path)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteFile(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { path ->
                sftp.rm(path)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun createThumbnailClient(): IThumbnailFTPClient {
        return SFTPThumbnailClient(config, context)
    }

    override fun createTransferFTPClient(
        poolContext: PoolContext,
        context: Context
    ): ITransferFTPClient {
        return SFTPTransferFTPClient(config, poolContext)
    }
}