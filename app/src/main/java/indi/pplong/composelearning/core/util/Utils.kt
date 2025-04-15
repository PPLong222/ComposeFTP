package indi.pplong.composelearning.core.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import indi.pplong.composelearning.core.base.FileType
import java.io.File
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 8:58 PM
 */

object DateUtil {
    fun getFormatDate(timeMills: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(timeMills)
        val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val zonedDataTime = ZonedDateTime.ofInstant(instant, zoneId)
        return zonedDataTime.format(timeFormat) ?: ""
    }
}

object FileUtil {
    private val SIZE_ARR = arrayOf("B", "KB", "MB", "GB")
    fun getFileSize(size: Long): String {

        var formatSize = size.toDouble()
        var i = 0
        while (formatSize / 1024F > 1 && i < SIZE_ARR.size - 1) {
            formatSize /= 1024F
            i++
        }

        return DecimalFormat("#.##").format(formatSize).plus(SIZE_ARR[i])
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    return it.getLong(sizeIndex)
                }
            }
        }
        return 0
    }

    fun getFileName(context: Context, uri: Uri): String {
        var fileName = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    fun getVideoThumbnailWithRetriever(
        context: Context,
        videoUri: Uri,
        width: Int,
        height: Int,
    ): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()

            context.contentResolver.openFileDescriptor(videoUri, "r")?.use {
                retriever.setDataSource(it.fileDescriptor)
                retriever.frameAtTime?.let { bitmap ->
                    Bitmap.createScaledBitmap(
                        bitmap,
                        width.dpToPx(context),
                        height.dpToPx(context),
                        true
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageThumbnailWithDecoder(
        contentResolver: ContentResolver,
        imageUri: Uri
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                // Android P 以下使用传统方式获取 Bitmap
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getContentUriInDownloadDir(
        context: Context,
        fileName: String
    ): Uri? {
        val resolver = context.contentResolver

        // 使用 ContentValues 创建文件的元数据
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 设置文件名
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                getMimeType(fileName)
            )
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        // 插入文件到 MediaStore 的 Downloads 目录
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )
        return uri
    }

    fun getFileType(fileName: String): FileType {
        val mimeType = getMimeType(fileName)
        if (mimeType.startsWith("video/")) return FileType.VIDEO
        if (mimeType.startsWith("image/")) return FileType.PNG
        return FileType.OTHER
    }

    fun isFileProviderUriExists(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getFileUriInDownloadDir(context: Context, fileName: String): Uri? {
        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloads, fileName)
        return file.toUri()
    }

    fun getTargetFileContentUriFromDir(
        context: Context,
        directorUri: Uri,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): Uri? {
        Log.d("123123", "getTargetFileContentUriFromDir: ${directorUri.toString()}")
        val pickedDir = DocumentFile.fromTreeUri(context, directorUri) ?: return null

        val existingFile = pickedDir.findFile(fileName)

        if (existingFile != null && existingFile.exists()) {
            return existingFile.uri
        }
        val newFile = pickedDir.createFile(mimeType, fileName)
        return newFile?.uri
    }
}

object ServerPortInfo {
    const val MAX_PORT = 65535
    const val MIN_PORT = 0
}

object VibrationUtil {
    fun triggerVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 100),
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}