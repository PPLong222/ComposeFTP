package indi.pplong.composelearning.ftp.state

import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.base.ITransferFTPClient

/**
 * @author PPLong
 * @date 4/19/25 12:04 PM
 */
data class TransferState(
    val downloadList: Set<ITransferFTPClient> = hashSetOf(),
    val uploadList: Set<ITransferFTPClient> = hashSetOf(),
    val idleClientList: List<ITransferFTPClient> = arrayListOf(),
    val pausedList: Set<TransferringFile> = hashSetOf()
)
