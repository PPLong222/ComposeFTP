package indi.pplong.composelearning.ftp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val port: Int?,
    val username: String,
    val password: String,
    val context: Context
) {
    protected val ftpClient: FTPClient = FTPClient()
    private val TAG = javaClass.name + "@" + host
    protected val mutex: Mutex = Mutex()

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

        customizeFTPClientSetting()
        return res
    }

    protected open fun customizeFTPClientSetting() {}

    suspend fun close() {
        mutex.withLock {
            ftpClient.logout()
            ftpClient.disconnect()
        }
    }

    suspend fun changePath(pathName: String): Boolean {
        return mutex.withLock {
            ftpClient.changeWorkingDirectory(pathName)
        }
    }

    suspend fun getCurrentPath(): String {
        return mutex.withLock {
            ftpClient.printWorkingDirectory() ?: ""
        }
    }

    suspend fun getFiles(): Array<out FTPFile> {
        return mutex.withLock {
            ftpClient.listFiles()
        }
    }

    suspend fun checkAndKeepAlive(): Boolean {
        return try {
            mutex.withLock {
                ftpClient.sendNoOp()
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isConnectionAlive(): Boolean {
        return mutex.withLock {
            try {
                ftpClient.sendNoOp()
            } catch (e: Exception) {
                false
            }
        }
    }
}