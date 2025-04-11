package indi.pplong.composelearning.ftp

import android.net.Uri
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.clients.CoreFTPClient
import indi.pplong.composelearning.ftp.clients.TransferFTPClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.net.ftp.FTPFile

/**
 * Description: Cache that holds a group of FTP Client for specific host.
 * @author PPLong
 * @date 9/30/24 12:24 PM
 */
class FTPClientCache @AssistedInject constructor(
    @Assisted val coreFTPClient: CoreFTPClient,
    private val transferFactory: TransferFTPClient.Factory
) : PoolContext {
    companion object {
        //        val maxSize: Int = MAX_CLIENTS_SIZE,
//        val maxAvailableSize: Int = MAX_AVAILABLE_CLIENTS_SIZE,
//        val longestActiveRemain: Long = LONGEST_ACTIVE_TIME,
        private const val MAX_CLIENTS_SIZE = 10
        private const val MAX_AVAILABLE_CLIENTS_SIZE = 2
        private const val LONGEST_ACTIVE_TIME = 10 * 1000L
    }

    val context = coreFTPClient.context
    var uploadQueue = MutableStateFlow(mutableSetOf<TransferFTPClient>())
    var downloadQueue = MutableStateFlow(mutableSetOf<TransferFTPClient>())
    var idledClientsQueue = MutableStateFlow(mutableListOf<TransferFTPClient>())

    /**
     * Feature Client:
     * FTPClient which handles the thumbnail of media files.
     */
    var thumbnailFTPClient = coreFTPClient.createThumbnailClient()


    val workingClientsCount: Int = downloadQueue.value.size + uploadQueue.value.size + 1
    val idleClientsCount: Int = idledClientsQueue.value.size
    val totalClientSize = workingClientsCount + idleClientsCount

    val hasUploadNode = uploadQueue.value.isNotEmpty()
    val hasDownloadNode = downloadQueue.value.isNotEmpty()
    suspend fun getCurrentPath() = coreFTPClient.getCurrentPath()

    suspend fun getAvailableTransferFTPClient(): TransferFTPClient? {
        var targetClient: TransferFTPClient? = null
        if (!idledClientsQueue.value.isEmpty()) {
            val tempQueue = idledClientsQueue.value.toMutableList()
            while (!tempQueue.isEmpty()) {
                val client: TransferFTPClient = tempQueue.first()
                tempQueue.removeAt(0)
                if (client.isConnectionAlive()) {
                    Log.d("TTTTT", "getAvailableTransferFTPClient:  reuse")
                    targetClient = client
                    break
                }
            }

            idledClientsQueue.update {
                tempQueue
            }
        }

        if (targetClient == null && totalClientSize < MAX_CLIENTS_SIZE) {
            val client = transferFactory.create(
                coreFTPClient.host,
                coreFTPClient.port,
                coreFTPClient.username,
                coreFTPClient.password,
                context,
                this
            )
            if (client.initClient()) {
                return client
            }
        }
        return targetClient
    }

    suspend fun downloadFile(fileInfo: FileItemInfo) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(fileInfo.pathPrefix)
            client.downloadFile(fileInfo)
        }
    }

    suspend fun uploadFile(transferringFile: TransferringFile, uri: Uri) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(transferringFile.transferredFileItem.remotePathPrefix)
            client.uploadFile(transferringFile, uri)
        }
    }

    suspend fun changePathAndGetFiles(path: String? = null): Array<out FTPFile> {
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

    override fun addToDownloadList(singleFTPClient: TransferFTPClient) {
        downloadQueue.update { set ->
            set.toMutableSet().apply { add(singleFTPClient) }
        }
    }

    override fun idleFromDownloadList(singleFTPClient: TransferFTPClient) {
        downloadQueue.update { set ->
            set.toMutableSet().apply { remove(singleFTPClient) }
        }
        idledClientsQueue.update {
            it.toMutableList().apply { add(singleFTPClient) }
        }
    }

    override fun addToUploadList(singleFTPClient: TransferFTPClient) {
        uploadQueue.update { set ->
            set.toMutableSet().apply { add(singleFTPClient) }
        }
    }

    override fun idleFromUploadList(singleFTPClient: TransferFTPClient) {
        uploadQueue.update { set ->
            set.toMutableSet().apply { remove(singleFTPClient) }
        }

        idledClientsQueue.update {
            it.toMutableList().apply { add(singleFTPClient) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(coreFTPClient: CoreFTPClient): FTPClientCache
    }
}