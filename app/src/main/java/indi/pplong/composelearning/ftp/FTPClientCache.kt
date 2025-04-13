package indi.pplong.composelearning.ftp

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.file.model.toTransferredFileItem
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.ftp.base.ICoreFTPClient
import indi.pplong.composelearning.ftp.base.IThumbnailFTPClient
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import indi.pplong.composelearning.ftp.ftp.CoreFTPClient
import indi.pplong.composelearning.ftp.sftp.SFTPCoreClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Description: Cache that holds a group of FTP Client for specific host.
 * @author PPLong
 * @date 9/30/24 12:24 PM
 */
class FTPClientCache(
    val config: FTPConfig,
    val context: Context,
    val transferredFileDao: TransferredFileDao
) : PoolContext {
    companion object {
        //        val maxSize: Int = MAX_CLIENTS_SIZE,
//        val maxAvailableSize: Int = MAX_AVAILABLE_CLIENTS_SIZE,
//        val longestActiveRemain: Long = LONGEST_ACTIVE_TIME,
        private const val MAX_CLIENTS_SIZE = 10
        private const val MAX_AVAILABLE_CLIENTS_SIZE = 2
        private const val LONGEST_ACTIVE_TIME = 10 * 1000L
    }

    lateinit var coreFTPClient: ICoreFTPClient

    var uploadQueue = MutableStateFlow(mutableSetOf<ITransferFTPClient>())
    var downloadQueue = MutableStateFlow(mutableSetOf<ITransferFTPClient>())
    var idledClientsQueue = MutableStateFlow(mutableListOf<ITransferFTPClient>())

    /**
     * Feature Client:
     * FTPClient which handles the thumbnail of media files.
     */
    lateinit var thumbnailFTPClient: IThumbnailFTPClient


    val workingClientsCount: Int = downloadQueue.value.size + uploadQueue.value.size + 1
    val idleClientsCount: Int = idledClientsQueue.value.size
    val totalClientSize = workingClientsCount + idleClientsCount
    private val clientPoolLock = Mutex()

    init {
        if (!config.isSFTP) {
            coreFTPClient = CoreFTPClient(config, context)
        } else {
            coreFTPClient = SFTPCoreClient(config, context)
        }
        thumbnailFTPClient = coreFTPClient.createThumbnailClient()
    }

    suspend fun getCurrentPath() = coreFTPClient.getCurrentPath()

    suspend fun getAvailableTransferFTPClient(): ITransferFTPClient? {
        return clientPoolLock.withLock {
            var targetClient: ITransferFTPClient? = null
            idledClientsQueue.getAndUpdate { queue ->
                val newQueue = queue.toMutableList()
                Log.d("TTTTT", "getAvailableTransferFTPClient ${newQueue.size}")

                while (!newQueue.isEmpty()) {
                    val client: ITransferFTPClient = newQueue.first()
                    newQueue.removeAt(0)

                    if (client.checkAndKeepAlive()) {
                        Log.d("TTTTT", "getAvailableTransferFTPClient:  reuse")
                        targetClient = client
                        break
                    }
                }
                newQueue
            }

            if (targetClient == null && totalClientSize < MAX_CLIENTS_SIZE) {
                val client = coreFTPClient.createTransferFTPClient(
                    this, context
                )
                if (client.initClient()) {
                    return client
                }
            }
            targetClient
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun downloadFile(fileInfo: FileItemInfo) {
        Log.d("123123", "downloadFile: Trye get FTRP")
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(fileInfo.pathPrefix)
            val uri = if (config.isSFTP) {
                FileUtil.getFileUriInDownloadDir(context, fileInfo.name)
            } else {
                FileUtil.getContentUriInDownloadDir(context, fileInfo.name)
            }
            client.download(
                fileInfo.toTransferredFileItem(config.host, 0, uri.toString()), onSuccess = {
                    transferredFileDao.insert(
                        fileInfo.toTransferredFileItem(
                            config.host,
                            transferType = 0,
                            localUri = uri.toString()
                        ).copy(
                            timeMills = System.currentTimeMillis()
                        )
                    )
                })
        }
    }

    suspend fun uploadFile(transferringFile: TransferredFileItem) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(transferringFile.remotePathPrefix)
            client.upload(
                transferringFile.copy(serverHost = config.host), onSuccess = {
                    transferredFileDao.insert(
                        transferringFile
                    )
                })
        }
    }

    suspend fun changePathAndGetFiles(path: String? = null): List<CommonFileInfo> {
        if (path != null) {
            val mainRes = coreFTPClient.changePath(path) && thumbnailFTPClient.changePath(path)
            if (!mainRes) {
                throw Exception()
            }
        }
        return coreFTPClient.list()
    }

    suspend fun launchThumbnailJob(fileName: String, key: String): Uri? {
        return thumbnailFTPClient.launchThumbnailWork(fileName, key)
    }

    override fun addToDownloadList(singleFTPClient: ITransferFTPClient) {
        downloadQueue.update { set ->
            set.toMutableSet().apply { add(singleFTPClient) }
        }
    }

    override fun idleFromDownloadList(singleFTPClient: ITransferFTPClient) {
        downloadQueue.update { set ->
            set.toMutableSet().apply { remove(singleFTPClient) }
        }
        idledClientsQueue.update {

            it.toMutableList().apply {
                add(singleFTPClient)
            }
        }
    }

    override fun addToUploadList(singleFTPClient: ITransferFTPClient) {
        uploadQueue.update { set ->
            set.toMutableSet().apply { add(singleFTPClient) }
        }
    }

    override fun idleFromUploadList(singleFTPClient: ITransferFTPClient) {
        uploadQueue.update { set ->
            set.toMutableSet().apply { remove(singleFTPClient) }
        }

        idledClientsQueue.update {
            it.toMutableList().apply { add(singleFTPClient) }
        }
    }
}