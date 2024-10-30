package indi.pplong.composelearning.core.cache

import android.util.Log
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.SingleFTPClient
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:24 PM
 */
class FTPClientCache(
    val coreFTPClient: SingleFTPClient,
    val maxSize: Int = MAX_CLIENTS_SIZE,
    val maxAvailableSize: Int = MAX_AVAILABLE_CLIENTS_SIZE,
    val longestActiveRemain: Long = LONGEST_ACTIVE_TIME
) {
    companion object {
        private const val MAX_CLIENTS_SIZE = 10
        private const val MAX_AVAILABLE_CLIENTS_SIZE = 2
        private const val LONGEST_ACTIVE_TIME = 10 * 1000L
    }

    var uploadQueue = MutableStateFlow(mutableSetOf<SingleFTPClient>())
    var downloadQueue = MutableStateFlow(mutableSetOf<SingleFTPClient>())
    private var idledClientsQueue: Queue<SingleFTPClient> = ConcurrentLinkedQueue()


    val workingClientsCount: Int = downloadQueue.value.size + uploadQueue.value.size + 1
    val idleClientsCount: Int = idledClientsQueue.size
    val totalClientSize = workingClientsCount + idleClientsCount

    val hasUploadNode = uploadQueue.value.isNotEmpty()
    val hasDownloadNode = downloadQueue.value.isNotEmpty()

    init {


    }

    private fun getAvailableFTPClient(): SingleFTPClient? {
        if (!idledClientsQueue.isEmpty()) {
            return idledClientsQueue.poll()
        }
        if (totalClientSize < MAX_CLIENTS_SIZE) {
            val client = coreFTPClient.copy()
            client.initClient()
            return client
        }
        return null
    }

    suspend fun downloadFile(outputStream: OutputStream, fileInfo: FileItemInfo) {
        getAvailableFTPClient()?.let { client ->
            client.changePath(fileInfo.pathPrefix)
            val newSet = downloadQueue.value.toMutableSet()
            newSet.add(client)
            downloadQueue.value = newSet
            Log.d("test", "downloadFile: Emit")
            client.downloadFile(outputStream, fileInfo)
        }
    }

    fun idleClientFromDownload(singleFTPClient: SingleFTPClient) {
        val newSet = downloadQueue.value.toMutableSet()
        newSet.remove(singleFTPClient)
        downloadQueue.value = newSet
    }

    fun idleClientFromUpload(singleFTPClient: SingleFTPClient) {
        val newSet = uploadQueue.value.toMutableSet()
        newSet.remove(singleFTPClient)
        uploadQueue.value = newSet
    }

    suspend fun uploadFile(transferringFile: TransferringFile, inputStream: InputStream) {
        getAvailableFTPClient()?.let { client ->
            client.changePath(transferringFile.transferredFileItem.remotePathPrefix)
            val newSet = downloadQueue.value.toMutableSet()
            newSet.add(client)
            downloadQueue.value = newSet
            Log.d("test", "downloadFile: Emit")
            client.uploadFile(transferringFile, inputStream)
        }
    }


}