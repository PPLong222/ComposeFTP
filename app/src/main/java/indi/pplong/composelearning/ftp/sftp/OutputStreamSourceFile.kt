package indi.pplong.composelearning.ftp.sftp

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import net.schmizz.sshj.xfer.LocalFileFilter
import net.schmizz.sshj.xfer.LocalSourceFile
import java.io.InputStream

/**
 * @author PPLong
 * @date 4/16/25 6:28 PM
 */
class OutputStreamSourceFile(
    val context: Context,
    val uri: Uri
) : LocalSourceFile {
    override fun getName(): String? {
        var name = "unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    override fun getLength(): Long {
        var size: Long = 0
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    override fun getInputStream(): InputStream? {
        return context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open inputStream")
    }

    override fun getPermissions(): Int {
        return 0b110100100 // 420
    }

    override fun isFile(): Boolean {
        return true
    }

    override fun isDirectory(): Boolean {
        return false
    }

    override fun getChildren(filter: LocalFileFilter?): Iterable<LocalSourceFile?>? {
        return null
    }

    override fun providesAtimeMtime(): Boolean {
        return false
    }

    override fun getLastAccessTime(): Long {
        return System.currentTimeMillis()
    }

    override fun getLastModifiedTime(): Long {
        return System.currentTimeMillis()
    }
}