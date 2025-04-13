package indi.pplong.composelearning.ftp

import indi.pplong.composelearning.ftp.base.ITransferFTPClient

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
}