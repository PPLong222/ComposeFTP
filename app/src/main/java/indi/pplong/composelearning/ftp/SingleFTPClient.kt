package indi.pplong.composelearning.ftp

import indi.pplong.composelearning.core.cache.FTPServerPool
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.io.CopyStreamAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.text.DecimalFormat
import java.time.ZoneId

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 9:04 PM
 */
class SingleFTPClient(
    val host: String,
    private val port: Int?,
    private val username: String,
    private val password: String,
    private val transferredFileDao: TransferredFileDao,
) {
    private val ftpClient: FTPClient = FTPClient()
    private var lastRecordTime: Long = 0L

    companion object {
        private const val FLOW_EMIT_INTERVAL = 250L
    }

    private var _transferFileFlow = MutableStateFlow<FileItemInfo>(FileItemInfo())
    val transferFileFlow = _transferFileFlow.asStateFlow()

    private var _uploadFileFlow = MutableStateFlow<TransferringFile>(TransferringFile())
    val uploadFileFlow = _uploadFileFlow.asStateFlow()

    fun initClient(
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ): Boolean {
        var res = false
        try {
            if (port == null) {
                ftpClient.connect(InetAddress.getByName(host))
            } else {
                ftpClient.connect(InetAddress.getByName(host), port)
            }
            res = ftpClient.login(username, password)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            onSuccess()
        } catch (e: IOException) {
            onFail()
        }
        return res
    }

    fun getList(): Array<FTPFile> = ftpClient.listFiles()


    fun close() {
        try {
            ftpClient.logout()
            ftpClient.disconnect()
        } catch (_: Exception) {

        }
    }

    val isAvailable: Boolean = ftpClient.isAvailable

    fun copy(): SingleFTPClient {
        return SingleFTPClient(host, port, username, password, transferredFileDao)
    }

    fun changePath(pathName: String) {
        ftpClient.changeWorkingDirectory(pathName)
    }

    fun getFileList() {
        ftpClient.listFiles()
    }

    fun getCurrentPath(): String {
        return ftpClient.printWorkingDirectory() ?: ""
    }

    fun changeAndGetFiles(
        targetPath: String,
        onBegin: () -> Unit = {},
        onSuccess: (Array<FTPFile>) -> Unit = {},
        onFail: () -> Unit = {},
    ) {
        try {
            onBegin()
            changePath(targetPath)
            val list = ftpClient.listFiles(targetPath)
            onSuccess(list)
        } catch (_: Exception) {
            onFail()
        }
    }

    suspend fun getFiles(
        targetPath: String = ftpClient.printWorkingDirectory()
    ): Array<out FTPFile>? {
        try {
            val listFiles = ftpClient.listFiles(targetPath)
            return listFiles
        } catch (_: Exception) {

        }
        return null

    }

    suspend fun uploadFile(transferringFile: TransferringFile, inputStream: InputStream) {

        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0
        _uploadFileFlow.value = transferringFile.copy()
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                runBlocking {
                    // Optimization: Not emit data too fast.
                    tempBytes += bytesTransferred
                    if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                        return@runBlocking
                    }
                    _uploadFileFlow.value = _uploadFileFlow.value.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / transferringFile.transferredFileItem.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                    println(tempBytes)
                    FTPServerPool.addToUploadList(this@SingleFTPClient)
                    lastRecordTime = System.currentTimeMillis()
                    tempBytes = 0
                }
            }
        }
        val result = ftpClient.storeUniqueFile(
            transferringFile.transferredFileItem.remotePathPrefix + "/" + transferringFile.transferredFileItem.remoteName,
            inputStream
        )
        if (result) {
            transferredFileDao.insert(
                transferringFile.transferredFileItem
            )
            _uploadFileFlow.value = _uploadFileFlow.value.copy(
                transferStatus = TransferStatus.Successful
            )
            FTPServerPool.removeFromUploadList(this@SingleFTPClient)
        } else {
            _uploadFileFlow.value = _uploadFileFlow.value.copy(
                transferStatus = TransferStatus.Failed
            )
        }
    }

    suspend fun downloadFile(
        outputStream: OutputStream, fileItemInfo: FileItemInfo
    ) {
        _transferFileFlow.value = fileItemInfo.copy()
        lastRecordTime = System.currentTimeMillis()
        var tempBytes = 0
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(
                totalBytesTransferred: Long,
                bytesTransferred: Int,
                streamSize: Long
            ) {
                runBlocking {
                    // Optimization: Not emit data too fast.
                    tempBytes += bytesTransferred
                    if (System.currentTimeMillis() - lastRecordTime < FLOW_EMIT_INTERVAL) {
                        return@runBlocking
                    }
                    _transferFileFlow.value = _transferFileFlow.value.copy(
                        transferStatus = TransferStatus.Transferring(
                            DecimalFormat("#.##").format(totalBytesTransferred * 1.0F / fileItemInfo.size)
                                .toFloat(),
                            tempBytes * 1000L / (System.currentTimeMillis() - lastRecordTime)
                        )
                    )
                    FTPServerPool.addToDownloadList(this@SingleFTPClient)
                    lastRecordTime = System.currentTimeMillis()
                    tempBytes = 0
                }
            }
        }
        val result = ftpClient.retrieveFile(fileItemInfo.name, outputStream)
        if (result) {
            transferredFileDao.insert(
                TransferredFileItem(
                    remoteName = fileItemInfo.name,
                    remotePathPrefix = fileItemInfo.pathPrefix,
                    timeMills = System.currentTimeMillis(),
                    timeZoneId = ZoneId.systemDefault().id,
                    serverHost = host,
                    size = fileItemInfo.size,
                    transferType = 0,
                    localUri = fileItemInfo.localUri
                )
            )
            _transferFileFlow.value = _transferFileFlow.value.copy(
                transferStatus = TransferStatus.Successful
            )
            FTPServerPool.removeFromDownloadList(this@SingleFTPClient)
        } else {
            _transferFileFlow.value = _transferFileFlow.value.copy(
                transferStatus = TransferStatus.Failed
            )
        }
    }

    // Only in core client
    fun deleteFile(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { fileName ->
                ftpClient.deleteFile(fileName)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Only in core client
    fun deleteDirectory(pathName: List<String>): Boolean {
        return try {
            pathName.forEach { fileName ->
                ftpClient.removeDirectory(fileName)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isTimeOut(): Boolean {
        try {
            ftpClient.printWorkingDirectory()
        } catch (e: FTPConnectionClosedException) {
            return true
        }
        return false
    }

    fun createDirectory(dirName: String): Boolean {
        return try {
            ftpClient.makeDirectory(dirName)
        } catch (_: Exception) {
            false
        }
    }
}