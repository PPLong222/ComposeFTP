package indi.pplong.composelearning.ftp

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.file.model.keyEquals
import indi.pplong.composelearning.core.file.model.toTransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.ftp.base.ICoreFTPClient
import indi.pplong.composelearning.ftp.base.IThumbnailFTPClient
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import indi.pplong.composelearning.ftp.ftp.CoreFTPClient
import indi.pplong.composelearning.ftp.sftp.SFTPCoreClient
import indi.pplong.composelearning.ftp.sftp.SFTPTransferFTPClient
import indi.pplong.composelearning.ftp.state.TransferState
import indi.pplong.composelearning.sys.Global
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val TAG = javaClass.name

    lateinit var coreFTPClient: ICoreFTPClient

//    var uploadQueue = MutableStateFlow(mutableSetOf<ITransferFTPClient>())
//    var downloadQueue = MutableStateFlow(mutableSetOf<ITransferFTPClient>())
//    var idledClientsQueue = MutableStateFlow(mutableListOf<ITransferFTPClient>())
//    var pausedQueue = MutableStateFlow(mutableListOf<TransferringFile>())

    private val _ftpClientState: MutableStateFlow<TransferState> = MutableStateFlow(TransferState())
    val ftpClientState = _ftpClientState.asStateFlow()

    /**
     * Feature Client:
     * FTPClient which handles the thumbnail of media files.
     */
    lateinit var thumbnailFTPClient: IThumbnailFTPClient

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
        var targetClient: ITransferFTPClient? = null
        clientPoolLock.withLock {
            val newQueue = _ftpClientState.value.idleClientList.toMutableList()
            Log.d(TAG, "getAvailableTransferFTPClient ${newQueue.size}")

            while (!newQueue.isEmpty()) {
                val client: ITransferFTPClient = newQueue.first()
                newQueue.removeAt(0)

                if (client.checkAndKeepAlive()) {
                    Log.d(TAG, "getAvailableTransferFTPClient:  reuse")
                    targetClient = client
                    break
                }
            }

            _ftpClientState.update {
                it.copy(idleClientList = newQueue)
            }
        }

        if (targetClient == null) {
            val client = coreFTPClient.createTransferFTPClient(
                this, context
            )
            if (client.initClient()) {
                targetClient = client
            }
        }
        return targetClient
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun downloadFile(fileInfo: CommonFileInfo, offsetBytes: Long = 0L) {
        Log.d("123123", "downloadFile: Trye get FTRP")
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(fileInfo.path)
            val uri = if (config.downloadDir != null) {
                FileUtil.getTargetFileContentUriFromDir(
                    context,
                    config.downloadDir.toUri(),
                    fileInfo.name
                )
            } else {
                FileUtil.getContentUriInDownloadDir(context, fileInfo.name)
            }
            var transferFile =
                fileInfo.toTransferredFileItem(config.key, 0, offsetBytes)
                    .copy(localUri = uri.toString())

            val id = transferredFileDao.insert(transferFile)
            transferFile = transferFile.copy(id = id)
            client.download(
                transferFile, onTaskFinish = {
                    transferredFileDao.update(it)
                }
            )
        }
    }

    suspend fun uploadFile(commonFileInfo: CommonFileInfo) {
        getAvailableTransferFTPClient()?.let { client ->
            client.changePath(commonFileInfo.path)
            val transferFile = commonFileInfo.toTransferredFileItem(config.key, 1, 0)
            client.upload(
                transferFile, onSuccess = {
                    transferredFileDao.insert(it)
                }
            )
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
        _ftpClientState.update {
            it.copy(downloadList = it.downloadList + singleFTPClient)
        }
    }

    override fun idleFromDownloadList(singleFTPClient: ITransferFTPClient) {
        _ftpClientState.update {
            it.copy(
                downloadList = it.downloadList - singleFTPClient,
                idleClientList = it.idleClientList + singleFTPClient
            )
        }
    }

    override fun addToUploadList(singleFTPClient: ITransferFTPClient) {
        _ftpClientState.update {
            it.copy(uploadList = it.uploadList + singleFTPClient)
        }
    }

    override fun idleFromUploadList(singleFTPClient: ITransferFTPClient) {
        _ftpClientState.update {
            it.copy(
                uploadList = it.uploadList - singleFTPClient,
                idleClientList = it.idleClientList + singleFTPClient
            )
        }
    }

    override fun addToPausedQueue(
        transferredFile: TransferringFile,
        client: SFTPTransferFTPClient
    ) {
        _ftpClientState.update {
            it.copy(
                pausedList = it.pausedList + transferredFile,
                downloadList = it.downloadList - client,
                uploadList = it.uploadList - client,
                idleClientList = it.idleClientList + client
            )
        }
    }

    suspend fun pauseTransferTask(fileInfo: TransferredFileItem) {
        Log.d(Global.GLOBAL_TAG, "pauseTransferTask: ")

        _ftpClientState.value.downloadList.forEach { client ->
            if (client.transferFlow().first().transferredFileItem.keyEquals(fileInfo)) {
                client.pause()
            }
        }
    }

    suspend fun resumeTransferTask(fileInfo: TransferringFile) {
        if (_ftpClientState.value.pausedList.find { it == fileInfo } == null) {
            return
        }

        getAvailableTransferFTPClient()?.let { client ->
            _ftpClientState.update {
                it.copy(
                    pausedList = it.pausedList - fileInfo,
                    downloadList = if (fileInfo.transferredFileItem.transferType == 0) it.downloadList + client else it.downloadList,
                    uploadList = if (fileInfo.transferredFileItem.transferType == 1) it.uploadList + client else it.uploadList,
                )
            }
            if (fileInfo.transferredFileItem.transferType == 0) {
                client.download(
                    fileInfo.transferredFileItem, onTaskFinish = {
                        transferredFileDao.update(it)
                    }
                )
            } else {
                client.upload(
                    fileInfo.transferredFileItem, onSuccess = {
                        transferredFileDao.update(it)
                    }
                )
            }
        }
    }
}