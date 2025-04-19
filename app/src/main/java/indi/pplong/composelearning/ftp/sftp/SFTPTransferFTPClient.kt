package indi.pplong.composelearning.ftp.sftp

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.StringUtil
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.PoolContext
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import indi.pplong.composelearning.sys.Global
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
    private var lastDatabaseRecordTime: Long = 0L
    private val TAG = javaClass.name

    @Volatile
    private var isPaused = false

    override suspend fun download(
        file: TransferredFileItem,
        onTaskFinish: suspend (TransferredFileItem) -> Unit,

        ) {
        sftp = ssh.newStatefulSFTPClient()
        isPaused = false
        lastRecordTime = System.currentTimeMillis()



        sftp.fileTransfer.transferListener = transferListener


        try {
            cacheContext.addToDownloadList(this)
            // Total Bytes Written
            var writesBytes = 0L
            // Speed Calculation
            var tempBytes = 0L
            Log.d(Global.GLOBAL_TAG, "download: try to download ${file}")

            val remoteFile = sftp.open(
                StringUtil.getFullPath(
                    fileName = file.remoteName,
                    path = file.remotePathPrefix
                )
            )
            context.contentResolver.openFileDescriptor(file.localUri.toUri(), "r")
                ?.use { localFileAttr ->
                    // May be different than db value
                    writesBytes = localFileAttr.statSize
                    Log.d(TAG, "download: stateSize: ${writesBytes}")
                }

            if (writesBytes > 0L) {
                progressFlow.update {
                    TransferringFile(
                        transferredFileItem = file,
                        transferStatus = TransferStatus.Paused(writesBytes)
                    )
                }
            } else {
                progressFlow.update {
                    TransferringFile(
                        transferredFileItem = file,
                        transferStatus = TransferStatus.Loading
                    )
                }
            }

            val attrs = remoteFile.fetchAttributes()

            remoteFile.ReadAheadRemoteFileInputStream(1, writesBytes, Long.MAX_VALUE)
                .use { input ->
                    context.contentResolver.openOutputStream(file.localUri.toUri(), "wa")
                        ?.use { output ->
                            val buffer = ByteArray(1024 * 1024 * 4)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                writesBytes += bytesRead
                                tempBytes += bytesRead
                                Log.d(TAG, "download: ${writesBytes}")
                                output.write(buffer, 0, bytesRead)
                                if (isPaused) {
                                    val updateValue = progressFlow.value.copy(
                                        transferStatus = TransferStatus.Paused(
                                            writesBytes
                                        )
                                    )
                                    progressFlow.update { updateValue }

                                    cacheContext.addToPausedQueue(
                                        updateValue,
                                        this
                                    )
                                    Log.d(Global.GLOBAL_TAG, "download: pasued")
                                    break
                                }

                                // Calculate Speed
                                if (System.currentTimeMillis() - lastRecordTime < ITransferFTPClient.FLOW_EMIT_INTERVAL) {

                                } else {
                                    progressFlow.update {
                                        it.copy(
                                            transferStatus = TransferStatus.Transferring(
                                                DecimalFormat("#.##").format(writesBytes * 1.0F / attrs.size)
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

            // Successfully written the whole file
            if (!isPaused) {
                // May throw exception when in customized dir
                if (config.downloadDir == null) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(file.localUri.toUri(), values, null, null)
                }

                progressFlow.update {
                    it.copy(
                        transferredFileItem = file.copy(isComplete = true),
                        transferStatus = TransferStatus.Successful
                    )
                }
            }
            Log.d(
                TAG,
                "download: finish: bytesWritten>:${writesBytes}, stateFileSize: ${attrs.size}"
            )
            onTaskFinish(
                file.copy(
                    timeMills = System.currentTimeMillis(),
                    isComplete = !isPaused
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            progressFlow.update {
                it.copy(transferStatus = TransferStatus.Failed)
            }
        } finally {
            sftp.close()
            cacheContext.idleFromDownloadList(this)
        }

    }

    override suspend fun upload(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    ) {
        sftp = ssh.newStatefulSFTPClient()
        isPaused = false

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
            sftp.put(
                OutputStreamSourceFile(context, file.localUri.toUri()),
                StringUtil.getFullPath(fileName = file.remoteName, path = file.remotePathPrefix)
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

    override fun pause() {
        isPaused = true
    }

    private val transferListener = object : TransferListener {
        override fun directory(name: String?): TransferListener {
            return this
        }

        override fun file(name: String?, size: Long): StreamCopier.Listener {
            var tempBytes = 0L
            return StreamCopier.Listener { bytesTransferred ->

            }
        }
    }
}