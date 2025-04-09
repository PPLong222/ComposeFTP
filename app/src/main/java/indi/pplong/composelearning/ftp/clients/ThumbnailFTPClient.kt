package indi.pplong.composelearning.ftp.clients

import android.content.Context
import android.net.Uri
import android.util.Log
import indi.pplong.composelearning.core.base.FileType
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.MD5Utils
import indi.pplong.composelearning.core.util.MediaUtils
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.ftp.BaseFTPClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.net.ProtocolCommandEvent
import org.apache.commons.net.ProtocolCommandListener
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

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
        private const val MAX_CACHE_FILE_SIZE = 4 * 1024 * 1024

        /** Max buffer to hold file cache: 3MB */
        private const val MAX_DOWNLOAD_SPEED = 3 * 1024 * 1024
    }

    private val TAG = javaClass.name + "@" + host

    suspend fun launchThumbnailWork(fileName: String, key: String): Uri? {
        return when (FileUtil.getFileType(fileName)) {
            FileType.PNG -> launchPhotoThumbnailWork(fileName, key)
            FileType.VIDEO -> launchVideoThumbnailWork(fileName, key)
            else -> null
        }
    }

    suspend fun launchVideoThumbnailWork(fileName: String, key: String): Uri? {
        var uri: Uri? = null
        Log.d(TAG, "launchThumbnailWork: Launch for $fileName + $key")
        val file = File(context.cacheDir, key)

        ftpClient.retrieveFileStream(fileName).use { inputStream ->
            BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
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
//            if (bitmap == null) {
//                ftpClient.completePendingCommand()
//                return null
//            }
            val finalFile =
                MD5Utils.bitmapToCompressedFile(context, bitmap!!, key)
            uri = finalFile.getContentUri(context)
        }
        // Remove origin preview file
        CoroutineScope(Dispatchers.IO).launch {
            file.delete()
            Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
        }
        // Important:
        ftpClient.completePendingCommand()
        Log.d(TAG, "launchVideoThumbnailWork: ${uri.toString()}")
        return uri
    }

    suspend fun launchPhotoThumbnailWork(fileName: String, key: String): Uri? {
        val file = File(context.cacheDir, key)
        val finalFile = File(context.cacheDir, "$key.jpg")
        ftpClient.retrieveFileStream(fileName).use { inputStream ->
            BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                }
            }
        }
        MediaUtils.compressImageQuality(file, finalFile, 80, context)
        // Remove origin preview file
        CoroutineScope(Dispatchers.IO).launch {
            file.delete()
            Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
        }
        // Important:
        ftpClient.completePendingCommand()
        return finalFile.getContentUri(context)
    }

    override fun customizeFTPClientSetting() {
        ftpClient.setControlKeepAliveTimeout(Duration.ofSeconds(10))
        ftpClient.setControlKeepAliveReplyTimeout(Duration.ofSeconds(10))

        val ftpKeepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            while (ftpClient.isAvailable) {
                delay(10_000) // Every 10 seconds
                try {
                    if (!ftpClient.sendNoOp()) {
                    }
                } catch (e: Exception) {
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