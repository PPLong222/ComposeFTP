package indi.pplong.composelearning.ftp.base

import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import kotlinx.coroutines.flow.Flow

/**
 * @author PPLong
 * @date 4/11/25 6:16 PM
 */
interface ITransferFTPClient : IBaseFTPClient {
    companion object {
        val FLOW_EMIT_INTERVAL = 250L
    }

    suspend fun download(
        file: TransferredFileItem,
        onSuccess: suspend (TransferredFileItem) -> Unit
    )

    suspend fun upload(file: TransferredFileItem, onSuccess: suspend (TransferredFileItem) -> Unit)

    fun transferFlow(): Flow<TransferringFile>
}