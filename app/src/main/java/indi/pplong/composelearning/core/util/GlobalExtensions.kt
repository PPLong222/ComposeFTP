package indi.pplong.composelearning.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import indi.pplong.composelearning.core.util.FileUtil.getImageThumbnailWithDecoder
import indi.pplong.composelearning.core.util.FileUtil.getVideoThumbnailWithRetriever
import java.io.File

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 8:26 PM
 */

fun Uri.isVideoFile(context: Context): Boolean {
    var mimeType = context.contentResolver.getType(this)
    if (mimeType == null) {
        mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(this.toString()).lowercase()
            )
    }
//    if (mimeType == null && DocumentsContract.isDocumentUri(context, this)) {
//        val docId = DocumentsContract.getDocumentId(this)
//        val type = docId.split(":").firstOrNull()
//        return type == "video" // 根据类型判断
//    }
    return mimeType?.startsWith("video/") ?: false
}

fun Uri.isImageFile(context: Context): Boolean {
    var mimeType = context.contentResolver.getType(this)
    if (mimeType == null) {
        mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(this.toString()).lowercase()
            )
    }
//    if (mimeType == null && DocumentsContract.isDocumentUri(context, this)) {
//        val docId = DocumentsContract.getDocumentId(this)
//        val type = docId.split(":").firstOrNull()
//        return type == "image" // 根据类型判断
//
//    }

    return mimeType?.startsWith("image/") ?: false
}

fun Uri.isMediaWithThumbnail(context: Context): Boolean {
    if (isImageFile(context) || isVideoFile(context)) {
        return true
    }
    return false
}

fun Uri.getThumbnail(context: Context): Bitmap? {
    if (isImageFile(context)) {
        return getImageThumbnailWithDecoder(context.contentResolver, this)
    } else if (isVideoFile(context)) {
        return getVideoThumbnailWithRetriever(
            context.contentResolver,
            this,
            48.dpToPx(context),
            48.dpToPx(context)
        )
    }
    return null
}

fun Uri.openFileWithUri(context: Context) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(this) ?: MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.toString()).lowercase())
    ?: return

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(this@openFileWithUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// File
fun File.getContentUri(context: Context): Uri? {
    return FileProvider.getUriForFile(context, "${context.packageName}.file_provider", this)
}

// Display
fun Int.dpToPx(context: Context): Int {
    return (context.resources.displayMetrics.density * this).toInt()
}