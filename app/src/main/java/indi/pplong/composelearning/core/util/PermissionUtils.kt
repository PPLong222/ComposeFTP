package indi.pplong.composelearning.core.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/**
 * Description:
 * @author PPLong
 * @date 10/24/24 2:28 PM
 */
object PermissionUtils {
    fun takePersistableUriPermission(contentResolver: ContentResolver, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
    }
}