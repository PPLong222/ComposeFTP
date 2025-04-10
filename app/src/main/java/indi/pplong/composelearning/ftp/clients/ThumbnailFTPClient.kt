package indi.pplong.composelearning.ftp.clients

import android.content.Context
import android.net.Uri
import android.util.Log
import indi.pplong.composelearning.core.base.FileType
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.MD5Utils
import indi.pplong.composelearning.core.util.MediaUtils
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.ftp.BaseFTPClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.apache.commons.net.ProtocolCommandEvent
import org.apache.commons.net.ProtocolCommandListener
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * Description: FTP Client that handles thumbnail generation
 * @author PPLong
 * @date 11/4/24 3:25 PM
 */
class ThumbnailFTPClient(
    host: String, port: Int?, username: String, password: String,
    context: Context
) : BaseFTPClient(
    host, port,
    username,
    password, context
) {
    companion object {
        /** Max media cache file size: 2MB */
        const val MAX_CACHE_FILE_SIZE = 4 * 1024 * 1024

        /** Max buffer to hold file cache: 3MB */
        const val MAX_DOWNLOAD_SPEED = 3 * 1024 * 1024
    }

    private val TAG = javaClass.name + "@" + host

    suspend fun launchThumbnailWork(fileName: String, key: String): Uri? {
        return when (FileUtil.getFileType(fileName)) {
            FileType.PNG -> mutex.withLock { launchPhotoThumbnailWork(fileName, key) }
            FileType.VIDEO -> mutex.withLock { launchVideoThumbnailWork(fileName, key) }
            else -> null
        }
    }

    suspend fun launchVideoThumbnailWork(fileName: String, key: String): Uri? {
        var uri: Uri? = null
        Log.d(TAG, "launchThumbnailWork: Launch for $fileName + $key")
        val file = File(context.cacheDir, key)
        try {
            ftpClient.retrieveFileStream(fileName).use { inputStream ->
                BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        if (totalBytesRead + bytesRead > MAX_CACHE_FILE_SIZE) {
                            outputStream.write(buffer, 0, MAX_CACHE_FILE_SIZE - totalBytesRead)
                            break
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                }
                val bitmap = FileUtil.getVideoThumbnailWithRetriever(
                    context,
                    file.getContentUri(context)!!,
                    48,
                    48
                )
                val finalFile =
                    MD5Utils.bitmapToCompressedFile(context, bitmap!!, key)
                uri = finalFile.getContentUri(context)
                GlobalCacheList.map.put(
                    key,
                    finalFile.getContentUri(context).toString()
                )
            }

            // Remove origin preview file
            CoroutineScope(Dispatchers.IO).launch {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Thumbnail job cancelled for $fileName")
            return null
        } catch (e: Exception) {
            Log.e(
                TAG,
                "launchVideoThumbnailWork: Exception when using stream.\n ${e.message} \n ${e.stackTrace}"
            )
            return null
        } finally {
            try {
                ftpClient.completePendingCommand()
            } catch (e: Exception) {
                Log.e(TAG, "Error in completePendingCommand: ${e.message}")
            }

            // 异步删除原始文件
            CoroutineScope(Dispatchers.IO).launch {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Deleted temp file: ${file.name}")
            }
        }

        Log.d(TAG, "launchVideoThumbnailWork: ${uri.toString()}")
        return uri
    }

    suspend fun launchPhotoThumbnailWork(fileName: String, key: String): Uri? {
        val file = File(context.cacheDir, key)
        val finalFile = File(context.cacheDir, "$key.jpg")
        try {
            ftpClient.retrieveFileStream(fileName).use { inputStream ->
                BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                }
            }

            // Remove origin preview file
            CoroutineScope(Dispatchers.IO).launch {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
            }

            MediaUtils.compressImageQuality(file, finalFile, 80, context)

            GlobalCacheList.map.put(
                key,
                finalFile.getContentUri(context).toString()
            )
        } catch (e: CancellationException) {
            Log.d(TAG, "Thumbnail job cancelled for $fileName")
            return null
        } catch (e: Exception) {
            Log.e(
                TAG,
                "launchPhotoThumbnailWork: Exception when using stream.\n ${e.message} \n ${e.stackTrace}"
            )
            return null
        } finally {
            try {
                ftpClient.completePendingCommand()
            } catch (e: Exception) {
                Log.e(TAG, "Error in completePendingCommand: ${e.message}")
            }
        }

        return finalFile.getContentUri(context)
    }

    override fun customizeFTPClientSetting() {
        ftpClient.setControlKeepAliveTimeout(Duration.ofSeconds(10))
        ftpClient.setControlKeepAliveReplyTimeout(Duration.ofSeconds(10))

        CoroutineScope(Dispatchers.IO).launch {
            while (ftpClient.isAvailable) {
                delay(10_000) // Every 10 seconds
                try {
                    if (!checkAndKeepAlive()) {
                        Log.d(TAG, "customizeFTPClientSetting: Connection OK.")
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