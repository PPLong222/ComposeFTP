package indi.pplong.composelearning.ftp.sftp

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.core.base.FileType
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.MD5Utils
import indi.pplong.composelearning.core.util.MediaUtils
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.base.IThumbnailFTPClient
import indi.pplong.composelearning.ftp.ftp.ThumbnailFTPClient.Companion.MAX_CACHE_FILE_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * @author PPLong
 * @date 4/11/25 6:49 PM
 */
class SFTPThumbnailClient @Inject constructor(
    config: FTPConfig,
    @ApplicationContext val context: Context
) : SFTPBaseClient(config), IThumbnailFTPClient {
    private val TAG = javaClass.name
    override suspend fun launchThumbnailWork(
        fileName: String,
        key: String
    ): Uri? {
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
        try {
            // TODO: meaning of [ReadAheadRemoteFileInputStream] ?
            sftp.open(fileName).ReadAheadRemoteFileInputStream(
                2, 0L, 1 * 1024 * 1024L
            ).use { inputStream ->
                BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                    val buffer = ByteArray(64 * 4096)
                    var bytesRead: Int
                    var totalBytesRead = 0
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        Log.d(TAG, "launchVideoThumbnailWork: data ${fileName} ${totalBytesRead}")
                        coroutineContext.ensureActive()
                        if (totalBytesRead + bytesRead > MAX_CACHE_FILE_SIZE) {
                            outputStream.write(buffer, 0, MAX_CACHE_FILE_SIZE - totalBytesRead)
                            break
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
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
            // 异步删除原始文件
            CoroutineScope(Dispatchers.IO).launch {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Deleted temp file: ${file.name}")
            }
        }

        Log.d(TAG, "launchVideoThumbnailWork: ${uri.toString()}")
        return uri
    }

    private suspend fun launchPhotoThumbnailWork(
        fileName: String, key: String
    ): Uri? {
        val file = File(context.cacheDir, key)
        val finalFile = File(context.cacheDir, "$key.jpg")
        try {
            sftp.open(fileName).ReadAheadRemoteFileInputStream(1).use { inputStream ->
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
            CoroutineScope(Dispatchers.IO).launch {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
            }
        }

        return finalFile.getContentUri(context)

    }
}