package indi.pplong.composelearning.ftp.ftp

import android.content.Context
import android.util.Log
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.PoolContext
import indi.pplong.composelearning.ftp.base.ICoreFTPClient
import indi.pplong.composelearning.ftp.base.IThumbnailFTPClient
import indi.pplong.composelearning.ftp.base.ITransferFTPClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.apache.commons.net.ProtocolCommandEvent
import org.apache.commons.net.ProtocolCommandListener
import java.time.Duration

/**
 * Description:
 * @author PPLong
 * @date 11/4/24 3:25 PM
 */
class CoreFTPClient(
    config: FTPConfig,
    val context: Context
) : BaseFTPClient(config), ICoreFTPClient {

    private val TAG = javaClass.name

    override suspend fun createDirectory(dirName: String): Boolean {
        return try {
            mutex.withLock { ftpClient.makeDirectory(dirName) }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteDirectory(pathName: List<String>): Boolean {
        return try {
            mutex.withLock {
                pathName.forEach { fileName ->
                    ftpClient.removeDirectory(fileName)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Only in core client
    override suspend fun deleteFile(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { fileName ->
                mutex.withLock {
                    ftpClient.deleteFile(fileName)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun createThumbnailClient(): IThumbnailFTPClient {
        return ThumbnailFTPClient(config, context)
    }

    override fun createTransferFTPClient(
        poolContext: PoolContext,
        context: Context
    ): ITransferFTPClient {
        return TransferFTPClient(config, poolContext, context)
    }

    override suspend fun renameFile(originalName: String, newName: String): Boolean {
        return mutex.withLock { ftpClient.rename(originalName, newName) }
    }

    override fun customizeFTPClientSetting() {
        ftpClient.setControlKeepAliveTimeout(Duration.ofSeconds(10))
        ftpClient.setControlKeepAliveReplyTimeout(Duration.ofSeconds(10))

        CoroutineScope(Dispatchers.IO).launch {
            while (ftpClient.isAvailable) {
                delay(10_000) // Every 10 seconds
                try {
                    if (!checkAndKeepAlive()) {
                        Log.d(TAG, "customizeFTPClientSetting: Connection OK")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "customizeFTPClientSetting: Connection lost ${e.message}")
                }
            }
        }
        val listener = object : ProtocolCommandListener {
            override fun protocolCommandSent(event: ProtocolCommandEvent?) {
                Log.d(
                    TAG,
                    "protocolCommandSent: command: ${event?.command}, message: ${event?.message}"
                )
            }

            override fun protocolReplyReceived(event: ProtocolCommandEvent?) {
                Log.d(
                    TAG,
                    "protocolReplyReceived: command: ${event?.command}, message: ${event?.message}"
                )
            }
        }
        ftpClient.addProtocolCommandListener(listener)
    }
}