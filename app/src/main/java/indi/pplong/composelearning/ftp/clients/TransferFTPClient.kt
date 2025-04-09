package indi.pplong.composelearning.ftp.clients

import android.annotation.SuppressLint
import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.ftp.BaseFTPClient
import indi.pplong.composelearning.ftp.PoolContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.apache.commons.net.io.CopyStreamAdapter
import java.io.InputStream
import java.text.DecimalFormat
import java.time.ZoneId

/**
 * Description:
 * @author PPLong
 * @date 11/4/24 3:43 PM
 */
class TransferFTPClient @AssistedInject constructor(
    @Assisted("host") host: String,
    @Assisted("port") port: Int?,
    @Assisted("username") username: String,
    @Assisted("password") password: String,
    @Assisted("appContext") context: Context,
    @Assisted("cacheContext") val cacheContext: PoolContext,
    private val transferredFileDao: TransferredFileDao

) : BaseFTPClient(host, port, username, password, context) {

    companion object {
        private const val FLOW_EMIT_INTERVAL = 250L
    }

    private var lastRecordTime: Long = 0L
    private var _transferFileFlow = MutableStateFlow<FileItemInfo>(FileItemInfo())
    val transferFileFlow = _transferFileFlow.asStateFlow()

    private var _uploadFileFlow = MutableStateFlow<TransferringFile>(TransferringFile())
    val uploadFileFlow = _uploadFileFlow.asStateFlow()

    suspend fun uploadFile(transferringFile: TransferringFile, inputStream: InputStream) {

        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0
        _uploadFileFlow.value = transferringFile.copy()
        cacheContext.addToUploadList(this@TransferFTPClient)
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                runBlocking {
                    // Optimization: Not emit data too fast.
                    tempBytes += bytesTransferred
                    if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                        return@runBlocking
                    }
                    _uploadFileFlow.value = _uploadFileFlow.value.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / transferringFile.transferredFileItem.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                    lastRecordTime = System.currentTimeMillis()
                    tempBytes = 0
                }
            }
        }
        var result = false
        inputStream.use {
            result = ftpClient.storeUniqueFile(
                transferringFile.transferredFileItem.remotePathPrefix + "/" + transferringFile.transferredFileItem.remoteName,
                inputStream
            )
        }

        if (result) {
            transferredFileDao.insert(
                transferringFile.transferredFileItem.copy(
                    timeMills = System.currentTimeMillis(),
                    timeZoneId = ZoneId.systemDefault().id
                )
            )
            _uploadFileFlow.value = _uploadFileFlow.value.copy(
                transferStatus = TransferStatus.Successful
            )
            cacheContext.idleFromUploadList(this@TransferFTPClient)
        } else {
            _uploadFileFlow.value = _uploadFileFlow.value.copy(
                transferStatus = TransferStatus.Failed
            )
        }
    }

    @SuppressLint("NewApi")
    suspend fun downloadFile(
        fileItemInfo: FileItemInfo
    ) {
        _transferFileFlow.value = fileItemInfo.copy()
        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0

        val uri = FileUtil.getFileUriInDownloadDir(context, fileItemInfo.name)
        cacheContext.addToDownloadList(this@TransferFTPClient)
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                runBlocking {
                    // Optimization: Not emit data too fast.
                    tempBytes += bytesTransferred
                    if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                        return@runBlocking
                    }
                    _transferFileFlow.value = _transferFileFlow.value.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / fileItemInfo.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )

                    lastRecordTime = System.currentTimeMillis()
                    tempBytes = 0
                }
            }
        }
        var result = false
        context.contentResolver.openOutputStream(uri!!).use { stream ->
            result = ftpClient.retrieveFile(fileItemInfo.name, stream)
        }

        if (result) {
            transferredFileDao.insert(
                TransferredFileItem(
                    remoteName = fileItemInfo.name,
                    remotePathPrefix = fileItemInfo.pathPrefix,
                    timeMills = System.currentTimeMillis(),
                    timeZoneId = ZoneId.systemDefault().id,
                    serverHost = host,
                    size = fileItemInfo.size,
                    transferType = 0,
                    localImageUri = fileItemInfo.localImageUri,
                    localUri = uri.toString()
                )
            )
            _transferFileFlow.value = _transferFileFlow.value.copy(
                transferStatus = TransferStatus.Successful
            )
            cacheContext.idleFromDownloadList(this@TransferFTPClient)
        } else {
            _transferFileFlow.value = _transferFileFlow.value.copy(
                transferStatus = TransferStatus.Failed
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("host") host: String,
            @Assisted("port") port: Int?,
            @Assisted("username") username: String,
            @Assisted("password") password: String,
            @Assisted("appContext") context: Context,
            @Assisted("cacheContext") cacheContext: PoolContext
        ): TransferFTPClient
    }
}