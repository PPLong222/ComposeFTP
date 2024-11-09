package indi.pplong.composelearning.ftp.clients

import android.content.Context
import indi.pplong.composelearning.ftp.BaseFTPClient

/**
 * Description:
 * @author PPLong
 * @date 11/4/24 3:25 PM
 */
class CoreFTPClient(
    host: String,
    port: Int?,
    username: String,
    password: String,
    context: Context
) : BaseFTPClient(host, port, username, password, context) {

    fun createDirectory(dirName: String): Boolean {
        return try {
            ftpClient.makeDirectory(dirName)
        } catch (_: Exception) {
            false
        }
    }

    fun deleteDirectory(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { fileName ->
                ftpClient.removeDirectory(fileName)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Only in core client
    fun deleteFile(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { fileName ->
                ftpClient.deleteFile(fileName)
            }
            true
        } catch (_: Exception) {
            false
        }
    }


    fun createThumbnailClient(): ThumbnailFTPClient {
        return ThumbnailFTPClient(host, port, username, password, context)
    }

    fun createTransferClient(): TransferFTPClient {
        return TransferFTPClient(host, port, username, password, context)
    }
}