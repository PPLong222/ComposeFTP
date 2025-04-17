package indi.pplong.composelearning.ftp.sftp

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
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
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.xfer.TransferListener
import java.text.DecimalFormat

/**
 * @author PPLong
 * @date 4/11/25 6:20 PM
 */
class SFTPTransferFTPClient(
    config: FTPConfig,
    val cacheContext: PoolContext,
    val context: Context
) : SFTPBaseClient(config), ITransferFTPClient {

    private val progressFlow = MutableStateFlow<TransferringFile>(TransferringFile())
    private var lastRecordTime: Long = 0L

    override suspend fun download(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    ) {
        sftp = ssh.newStatefulSFTPClient()
        lastRecordTime = System.currentTimeMillis()
        progressFlow.update {
            TransferringFile(
                transferredFileItem = file,
                transferStatus = TransferStatus.Loading
            )
        }
        sftp.fileTransfer.transferListener = transferListener


        try {
            cacheContext.addToDownloadList(this)

            sftp.fileTransfer.download(
                file.remotePathPrefix + "/" + file.remoteName,
                OutputStreamDestFile(context, file.localUri.toUri())
            )
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(file.localUri.toUri(), values, null, null)

            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Successful)
            }
            onSuccess(file.copy(timeMills = System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Failed)
            }
        } finally {
            sftp.close()
            delay(500)
            cacheContext.idleFromDownloadList(this)
        }

    }

    override suspend fun upload(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    ) {
        lastRecordTime = System.currentTimeMillis()
        progressFlow.update {
            TransferringFile(
                transferredFileItem = file,
                transferStatus = TransferStatus.Loading
            )
        }

        sftp.fileTransfer.transferListener = transferListener
        try {
            cacheContext.addToUploadList(this)
            sftp.fileTransfer.upload(
                OutputStreamSourceFile(context, file.localUri.toUri()),
                file.remotePathPrefix + "/" + file.remoteName
            )
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Successful)
            }
            onSuccess(file.copy(timeMills = System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Failed)
            }
        } finally {
            sftp.close()
            delay(500)
            cacheContext.idleFromUploadList(this)
        }
    }

    override fun transferFlow(): Flow<TransferringFile> {
        return progressFlow
    }

    private val transferListener = object : TransferListener {
        override fun directory(name: String?): TransferListener {
            return this
        }

        override fun file(name: String?, size: Long): StreamCopier.Listener {
            var tempBytes = 0L
            return StreamCopier.Listener { bytesTransferred ->
                tempBytes += bytesTransferred
                Log.d("123123", "file: bytes: ${bytesTransferred}")
                if (System.currentTimeMillis() - lastRecordTime < ITransferFTPClient.FLOW_EMIT_INTERVAL) {
                    return@Listener
                }
                progressFlow.update {
                    it.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(bytesTransferred * 1.0F / size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                }
                lastRecordTime = System.currentTimeMillis()
                tempBytes = 0
            }
        }
    }
}