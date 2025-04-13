package indi.pplong.composelearning.ftp.ftp

import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.core.file.model.toCommonFileInfo
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.base.IBaseFTPClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.net.InetAddress

/**
 * Description: BaseFTPClient, contains basic operation on FTP
 * Version: 1.0, without wrapping exception
 * @author PPLong
 * @date 11/4/24 3:23 PM
 */
open class BaseFTPClient(
    val config: FTPConfig
) : IBaseFTPClient {
    protected val ftpClient: FTPClient = FTPClient()
    private val TAG = javaClass.name + "@" + config.host + "@" + config.username
    protected val mutex: Mutex = Mutex()

    override suspend fun initClient(): Boolean {
        // Enable UTF-8 to avoid coding mess
        return try {
            var res = false
            ftpClient.autodetectUTF8 = true
            if (config.port == null) {
                ftpClient.connect(InetAddress.getByName(config.host))
            } else {
                ftpClient.connect(InetAddress.getByName(config.host), config.port)
            }
            res = ftpClient.login(config.username, config.password)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            customizeFTPClientSetting()
            res
        } catch (e: Exception) {
            false
        }
    }

    protected open fun customizeFTPClientSetting() {}

    override suspend fun close() {
        mutex.withLock {
            ftpClient.logout()
            ftpClient.disconnect()
        }
    }

    override suspend fun list(): List<CommonFileInfo> {
        return mutex.withLock {
            val parent = ftpClient.printWorkingDirectory()
            ftpClient.listFiles().map {
                it.toCommonFileInfo(
                    path = parent
                )
            }
        }
    }

    override suspend fun changePath(pathName: String): Boolean {
        return mutex.withLock {
            ftpClient.changeWorkingDirectory(pathName)
        }
    }

    override suspend fun getCurrentPath(): String {
        return mutex.withLock {
            ftpClient.printWorkingDirectory() ?: ""
        }
    }

    override suspend fun checkAndKeepAlive(): Boolean {
        return try {
            mutex.withLock {
                ftpClient.sendNoOp()
            }
        } catch (e: Exception) {
            false
        }
    }
}