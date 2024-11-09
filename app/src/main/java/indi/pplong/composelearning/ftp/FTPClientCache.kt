package indi.pplong.composelearning.ftp

import android.net.Uri
import android.util.Log
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.clients.CoreFTPClient
import indi.pplong.composelearning.ftp.clients.TransferFTPClient
import kotlinx.coroutines.flow.MutableStateFlow
import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Description: Cache that holds a group of FTP Client for specific host.
 * @author PPLong
 * @date 9/30/24 12:24 PM
 */
class FTPClientCache(
    val coreFTPClient: CoreFTPClient,
    val maxSize: Int = MAX_CLIENTS_SIZE,
    val maxAvailableSize: Int = MAX_AVAILABLE_CLIENTS_SIZE,
    val longestActiveRemain: Long = LONGEST_ACTIVE_TIME
) {
    companion object {
        private const val MAX_CLIENTS_SIZE = 10
        private const val MAX_AVAILABLE_CLIENTS_SIZE = 2
        private const val LONGEST_ACTIVE_TIME = 10 * 1000L
    }

    val context = coreFTPClient.context
    var uploadQueue = MutableStateFlow(mutableSetOf<TransferFTPClient>())
    var downloadQueue = MutableStateFlow(mutableSetOf<TransferFTPClient>())
    private var idledClientsQueue: Queue<TransferFTPClient> = ConcurrentLinkedQueue()

    /**
     * Feature Client:
     * FTPClient which handles the thumbnail of media files.
     */
    var thumbnailFTPClient = coreFTPClient.createThumbnailClient()


    val workingClientsCount: Int = downloadQueue.value.size + uploadQueue.value.size + 1
    val idleClientsCount: Int = idledClientsQueue.size
    val totalClientSize = workingClientsCount + idleClientsCount

    val hasUploadNode = uploadQueue.value.isNotEmpty()
    val hasDownloadNode = downloadQueue.value.isNotEmpty()
    fun getCurrentPath() = coreFTPClient.getCurrentPath()

    init {


    }

    fun getAvailableTransferFTPClient(): TransferFTPClient? {
        if (!idledClientsQueue.isEmpty()) {
            return idledClientsQueue.poll()
        }
        if (totalClientSize < MAX_CLIENTS_SIZE) {
            val client = coreFTPClient.createTransferClient()
            if (client.initClient()) {
                return client
            }
        }
        return null
    }

    suspend fun downloadFile(fileInfo: FileItemInfo) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(fileInfo.pathPrefix)
            val newSet = downloadQueue.value.toMutableSet()
            newSet.add(client)
            downloadQueue.value = newSet
            Log.d("test", "downloadFile: Emit")
            client.downloadFile(fileInfo)
        }
    }

    suspend fun downloadFile(fileInfo: FileItemInfo, localUri: String) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(fileInfo.pathPrefix)
            val newSet = downloadQueue.value.toMutableSet()
            newSet.add(client)
            downloadQueue.value = newSet
            Log.d("test", "downloadFile: Emit")
            client.downloadFile(fileInfo, localUri)
        }
    }

    fun idleClientFromDownload(singleFTPClient: TransferFTPClient) {
        val newSet = downloadQueue.value.toMutableSet()
        newSet.remove(singleFTPClient)
        downloadQueue.value = newSet
    }

    fun idleClientFromUpload(singleFTPClient: TransferFTPClient) {
        val newSet = uploadQueue.value.toMutableSet()
        newSet.remove(singleFTPClient)
        uploadQueue.value = newSet
    }

    suspend fun uploadFile(transferringFile: TransferringFile, inputStream: InputStream) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(transferringFile.transferredFileItem.remotePathPrefix)
            val newSet = downloadQueue.value.toMutableSet()
            newSet.add(client)
            downloadQueue.value = newSet
            Log.d("test", "downloadFile: Emit")
            client.uploadFile(transferringFile, inputStream)
        }
    }

    fun changePathAndGetFiles(path: String? = null): Array<out FTPFile> {
        if (path != null) {
            val mainRes = coreFTPClient.changePath(path) && thumbnailFTPClient.changePath(path)
            if (!mainRes) {
                throw Exception()
            }
        }
        return coreFTPClient.getFiles()
    }

    suspend fun launchThumbnailJob(fileName: String, key: String): Uri? {
        return thumbnailFTPClient.launchThumbnailWork(fileName, key)
    }
}