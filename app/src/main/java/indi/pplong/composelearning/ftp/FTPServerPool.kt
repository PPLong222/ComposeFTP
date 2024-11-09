package indi.pplong.composelearning.ftp

import android.content.Context
import indi.pplong.composelearning.core.cache.thumbnail.ThumbnailCacheDao
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.ftp.clients.CoreFTPClient
import indi.pplong.composelearning.ftp.clients.TransferFTPClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:52 PM
 */
class FTPServerPool(val context: Context) {
    private val _serverFTPMap = MutableStateFlow<Map<String, FTPClientCache>>(mapOf())
    val serverFTPMap = _serverFTPMap

    private val _downloadFTPSet = MutableStateFlow<MutableSet<TransferFTPClient>>(mutableSetOf())
    val downloadFTPList = _downloadFTPSet.asStateFlow()

    private val _uploadFTPSet = MutableStateFlow<MutableSet<TransferFTPClient>>(mutableSetOf())
    val uploadFTPList = _uploadFTPSet.asStateFlow()

    fun getAllCache(): List<FTPClientCache> {
        return _serverFTPMap.value.values.toList()
    }

    fun getCacheByHost(host: String): FTPClientCache? {
        return _serverFTPMap.value[host]
    }

    fun putCacheByHost(host: String, ftpClient: CoreFTPClient) {

        _serverFTPMap.update { map ->
            map.toMutableMap().apply {
                put(host, FTPClientCache(ftpClient))
                println("Put Cache ByHost")
            }
        }
    }

    fun initNewCache(
        host: String,
        port: Int,
        user: String,
        password: String,
        transferredFileDao: TransferredFileDao,
        thumbnailCacheDao: ThumbnailCacheDao
    ): Boolean {
        val createNewClient =
            createNewClient(host, port, user, password, transferredFileDao, thumbnailCacheDao)
        val ftpClientCache = FTPClientCache(coreFTPClient = createNewClient)
        if (createNewClient.initClient() && ftpClientCache.thumbnailFTPClient.initClient()) {
            _serverFTPMap.update { map ->
                map.toMutableMap().apply {
                    put(host, ftpClientCache)
                    println("Put Cache ByHost")
                }
            }
            return true
        }
        return false
    }

    fun createNewClient(
        host: String,
        port: Int,
        user: String,
        password: String,
        transferredFileDao: TransferredFileDao,
        thumbnailCacheDao: ThumbnailCacheDao
    ): CoreFTPClient {
        return CoreFTPClient(
            host,
            port,
            user,
            password,
            context
        )
    }

    fun addToDownloadList(singleFTPClient: TransferFTPClient) {
        _downloadFTPSet.value = _downloadFTPSet.value.toMutableSet().apply { add(singleFTPClient) }

    }

    fun removeFromDownloadList(singleFTPClient: TransferFTPClient) {
        _serverFTPMap.value[singleFTPClient.host]?.idleClientFromDownload(singleFTPClient)
        _downloadFTPSet.value =
            _downloadFTPSet.value.toMutableSet().apply { remove(singleFTPClient) }
    }

    fun addToUploadList(singleFTPClient: TransferFTPClient) {
        _uploadFTPSet.value = _uploadFTPSet.value.toMutableSet().apply { add(singleFTPClient) }
    }

    fun removeFromUploadList(singleFTPClient: TransferFTPClient) {
        _serverFTPMap.value[singleFTPClient.host]?.idleClientFromUpload(singleFTPClient)
        _uploadFTPSet.value =
            _uploadFTPSet.value.toMutableSet().apply { remove(singleFTPClient) }
    }

    suspend fun testHostServerConnectivity(
        host: String,
        passwd: String,
        user: String,
        port: Int,
        transferredFileDao: TransferredFileDao,
        thumbnailCacheDao: ThumbnailCacheDao
    ): Boolean {
        val ftpClient =
            BaseFTPClient(
                host,
                port,
                user,
                passwd,
                context
            )
        val res = ftpClient.initClient()
        ftpClient.close()
        return res
    }
}