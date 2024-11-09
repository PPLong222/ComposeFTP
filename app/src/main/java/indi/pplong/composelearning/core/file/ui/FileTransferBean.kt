package indi.pplong.composelearning.core.file.ui

import android.net.Uri
import indi.pplong.composelearning.core.file.model.FileItemInfo
import java.io.InputStream
import java.io.OutputStream

/**
 * Description:
 * @author PPLong
 * @date 11/7/24 8:46 PM
 */
data class FileDownloadBean(
    val inputStream: OutputStream,
    val fileItemInfo: FileItemInfo,
    val uri: Uri
)

data class FileUploadBean(
    val inputStream: InputStream,
    val fileItemInfo: FileItemInfo,
    val uri: Uri
)
