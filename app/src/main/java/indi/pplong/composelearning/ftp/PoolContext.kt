package indi.pplong.composelearning.ftp

import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import indi.pplong.composelearning.ftp.sftp.SFTPTransferFTPClient

/**
 * Description:
 * @author PPLong
 * @date 4/7/25 12:14 PM
 */
interface PoolContext {
    fun addToDownloadList(singleFTPClient: ITransferFTPClient)

    fun idleFromDownloadList(singleFTPClient: ITransferFTPClient)

    fun addToUploadList(singleFTPClient: ITransferFTPClient)

    fun idleFromUploadList(singleFTPClient: ITransferFTPClient)

    fun addToPausedQueue(transferredFile: TransferringFile, client: SFTPTransferFTPClient)
}