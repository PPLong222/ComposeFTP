package indi.pplong.composelearning.ftp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.net.InetAddress

/**
 * Description: BaseFTPClient, contains basic operation on FTP
 * Version: 1.0, without wrapping exception
 * @author PPLong
 * @date 11/4/24 3:23 PM
 */
open class BaseFTPClient(
    val host: String,
    protected val port: Int?,
    protected val username: String,
    protected val password: String,
    val context: Context
) {
    protected val ftpClient: FTPClient = FTPClient()
    private val TAG = javaClass.name + "@" + host

    private val ftpClientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initClient(): Boolean {
        var res = false
        // Enable UTF-8 to avoid coding mess
        ftpClient.autodetectUTF8 = true
        if (port == null) {
            ftpClient.connect(InetAddress.getByName(host))
        } else {
            ftpClient.connect(InetAddress.getByName(host), port)
        }
        res = ftpClient.login(username, password)
        ftpClient.enterLocalPassiveMode()
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
        return res
    }

    suspend fun close() {
        ftpClient.logout()
        ftpClient.disconnect()
    }

    fun changePath(pathName: String): Boolean {
        return ftpClient.changeWorkingDirectory(pathName)
    }

    fun getCurrentPath(): String {
        return ftpClient.printWorkingDirectory() ?: ""
    }

    fun getFiles(): Array<out FTPFile> {
        return ftpClient.listFiles()
    }

}