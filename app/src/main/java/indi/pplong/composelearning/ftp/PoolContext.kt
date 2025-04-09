package indi.pplong.composelearning.ftp

import indi.pplong.composelearning.ftp.clients.TransferFTPClient

/**
 * Description:
 * @author PPLong
 * @date 4/7/25 12:14 PM
 */
interface PoolContext {
    fun addToDownloadList(singleFTPClient: TransferFTPClient)

    fun idleFromDownloadList(singleFTPClient: TransferFTPClient)

    fun addToUploadList(singleFTPClient: TransferFTPClient)

    fun idleFromUploadList(singleFTPClient: TransferFTPClient)
}