package indi.pplong.composelearning.ftp.sftp

import android.util.Log
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.ftp.FTPConfig
import indi.pplong.composelearning.ftp.base.IBaseFTPClient
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

/**
 * @author PPLong
 * @date 4/11/25 6:02 PM
 */
open class SFTPBaseClient(
    val config: FTPConfig
) : IBaseFTPClient {
    lateinit var ssh: SSHClient
    lateinit var sftp: StatefulSFTPClient
    private val TAG = javaClass.name
    override suspend fun initClient(): Boolean {
        return try {
            ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(config.host, config.port ?: 22)
            ssh.authPassword(config.username, config.password)
            sftp = ssh.newStatefulSFTPClient()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "initClient: ")
            false
        }
    }

    override suspend fun close() {
        sftp.close()
    }

    override suspend fun list(): List<CommonFileInfo> {
        return sftp.ls().map { file ->
            CommonFileInfo(
                name = file.name,
                path = file.parent,
                isDir = file.isDirectory,
                mtime = file.attributes.mtime * 1000L,
                size = file.attributes.size,
                user = file.attributes.uid.toString()
            )
        }
    }

    override suspend fun getCurrentPath(): String? {
        return sftp.pwd()
    }

    override suspend fun checkAndKeepAlive(): Boolean {
        return true
    }

    override suspend fun changePath(pathName: String): Boolean {
        return try {
            sftp.cd(pathName)
            true
        } catch (e: Exception) {
            false
        }
    }
}