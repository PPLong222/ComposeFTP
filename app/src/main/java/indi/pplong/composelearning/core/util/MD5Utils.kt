package indi.pplong.composelearning.core.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * Description:
 * @author PPLong
 * @date 11/3/24 4:09 PM
 */
object MD5Utils {
    fun digestMD5AsString(input: ByteArray): String {
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException(e)
        }
        return bytesToHex(md.digest(input))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    suspend fun bitmapToCompressedFile(context: Context, bitmap: Bitmap, key: String): File {
        val cacheKey = key

        // 使用 Coil 加载并缓存压缩后的图片
        val tempFile = File(context.cacheDir, "$cacheKey.jpg")
        FileOutputStream(tempFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)  // 设置压缩质量
        }
        return tempFile
    }

}