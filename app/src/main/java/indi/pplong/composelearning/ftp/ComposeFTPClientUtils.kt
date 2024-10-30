package indi.pplong.composelearning.ftp

import indi.pplong.composelearning.core.file.model.TransferredFileDao

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 4:21 PM
 */
object ComposeFTPClientUtils {
    fun testHostServerConnectivity(
        host: String,
        passwd: String,
        user: String,
        port: Int,
        transferredFileDao: TransferredFileDao
    ): Boolean {
        val ftpClient = SingleFTPClient(host, port, user, passwd, transferredFileDao)
        val res = ftpClient.initClient()
        ftpClient.close()
        return res
    }
}