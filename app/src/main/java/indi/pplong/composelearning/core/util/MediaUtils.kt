package indi.pplong.composelearning.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Description:
 * @author PPLong
 * @date 11/7/24 2:59 PM
 */
object MediaUtils {
    fun compressImageQuality(originalFile: File, outputFile: File, quality: Int, context: Context) {
        val width = 48
        val height = 48
        val scaledBitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeFile(originalFile.path),
            width.dpToPx(context),
            height.dpToPx(context),
            true
        )
        FileOutputStream(outputFile).use { outputStream ->
            scaledBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream
            ) // quality: 0-100
        }
        scaledBitmap.recycle()
    }
}