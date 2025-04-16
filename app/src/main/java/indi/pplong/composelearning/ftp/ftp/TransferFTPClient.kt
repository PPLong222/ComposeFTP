package indi.pplong.composelearning.ftp.ftp

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.PoolContext
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.net.ProtocolCommandEvent
import org.apache.commons.net.ProtocolCommandListener
import org.apache.commons.net.io.CopyStreamAdapter
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 11/4/24 3:43 PM
 */
class TransferFTPClient @Inject constructor(
    config: FTPConfig,
    val cacheContext: PoolContext,
    @ApplicationContext val context: Context,
) : BaseFTPClient(config), ITransferFTPClient {

    companion object {
        private const val FLOW_EMIT_INTERVAL = 250L
    }

    private val TAG = javaClass.name
    private var lastRecordTime: Long = 0L
    private val progressFlow = MutableStateFlow<TransferringFile>(TransferringFile())

    override fun customizeFTPClientSetting() {
        val listener = object : ProtocolCommandListener {
            override fun protocolCommandSent(event: ProtocolCommandEvent?) {
                Log.d(
                    TAG,
                    "protocolCommandSent: command: ${event?.command}, message: ${event?.message}"
                )
            }

            override fun protocolReplyReceived(event: ProtocolCommandEvent?) {
                Log.d(
                    TAG,
                    "protocolReplyReceived: command: ${event?.command}, message: ${event?.message}"
                )
            }
        }
        ftpClient.addProtocolCommandListener(listener)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun download(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    ) {
        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0
        progressFlow.update {
            TransferringFile(
                transferredFileItem = file,
                transferStatus = TransferStatus.Loading
            )
        }

        cacheContext.addToDownloadList(this@TransferFTPClient)
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                // Optimization: Not emit data too fast.
                tempBytes += bytesTransferred
                if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                    return
                }
                progressFlow.update {
                    it.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / file.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                }

                lastRecordTime = System.currentTimeMillis()
                tempBytes = 0
            }
        }
        var result = false
        try {
            context.contentResolver.openOutputStream(file.localUri.toUri())?.use { stream ->
                result = ftpClient.retrieveFile(file.remoteName, stream)
            }


            if (result) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(file.localUri.toUri(), values, null, null)

                progressFlow.update {
                    it.copy(transferStatus = TransferStatus.Successful)
                }
                onSuccess(file)
            } else {
                progressFlow.update {
                    it.copy(transferStatus = TransferStatus.Failed)
                }
            }
        } catch (e: Exception) {
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Failed)
            }
        } finally {
            delay(500)
            cacheContext.idleFromDownloadList(this@TransferFTPClient)
        }
    }

    override suspend fun upload(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    ) {
        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0
        progressFlow.update {
            TransferringFile(
                transferredFileItem = file,
                transferStatus = TransferStatus.Loading
            )
        }
        cacheContext.addToUploadList(this@TransferFTPClient)
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                // Optimization: Not emit data too fast.
                tempBytes += bytesTransferred
                if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                    return
                }
                progressFlow.update {
                    it.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / file.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                }

                lastRecordTime = System.currentTimeMillis()
                tempBytes = 0

            }
        }
        var result = false
        try {
            context.contentResolver.openInputStream(file.localUri.toUri())?.use { stream ->
                result = ftpClient.storeFile(
                    file.remoteName,
                    stream
                )
            }

            if (result) {
                progressFlow.update {
                    it.copy(transferStatus = TransferStatus.Successful)
                }
                onSuccess(file)
            } else {
                progressFlow.update {
                    it.copy(transferStatus = TransferStatus.Failed)
                }
            }
        } catch (e: Exception) {
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Failed)
            }
        } finally {
            delay(500)
            cacheContext.idleFromUploadList(this@TransferFTPClient)
        }
    }

    override fun transferFlow(): Flow<TransferringFile> {
        return progressFlow
    }
}