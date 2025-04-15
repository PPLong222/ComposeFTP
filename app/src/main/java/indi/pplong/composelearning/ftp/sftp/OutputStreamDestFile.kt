package indi.pplong.composelearning.ftp.sftp

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import net.schmizz.sshj.xfer.LocalDestFile
import java.io.OutputStream

/**
 * @author PPLong
 * @date 4/14/25 9:59 PM
 */
class OutputStreamDestFile(
    val context: Context,
    val uri: Uri?
) : LocalDestFile {
    override fun getLength(): Long {
        return try {
            uri?.let {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun getOutputStream(): OutputStream? {
        return getOutputStream(false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun getOutputStream(append: Boolean): OutputStream? {
        val mode = if (append) "wa" else "w"

        return uri?.let { context.contentResolver.openOutputStream(it, mode) }
    }

    override fun getChild(name: String?): LocalDestFile? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun getTargetFile(filename: String?): LocalDestFile? {
        return this
    }

    override fun getTargetDirectory(dirname: String?): LocalDestFile? {
        return null
    }

    override fun setPermissions(perms: Int) {

    }

    override fun setLastAccessedTime(t: Long) {
    }

    override fun setLastModifiedTime(t: Long) {
    }
}