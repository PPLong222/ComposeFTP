package indi.pplong.composelearning.ftp

import java.io.File

/**
 * Description:
 * @author PPLong
 * @date 4/11/25 4:51 PM
 */
interface IFTPClient {
    suspend fun connect(): Boolean

    suspend fun disconnect()

    suspend fun upload(localFile: File, remotePath: String): Boolean

    suspend fun download(remotePath: String, localFile: File): Boolean

    suspend fun listFiles(remotePath: String)

    suspend fun deleteFile(remotePath: String): Boolean

    fun isConnected(): Boolean
}