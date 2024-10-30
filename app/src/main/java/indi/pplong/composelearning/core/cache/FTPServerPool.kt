package indi.pplong.composelearning.core.cache

import indi.pplong.composelearning.ftp.SingleFTPClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:52 PM
 */
object FTPServerPool {
    private val _serverFTPMap = MutableStateFlow<Map<String, FTPClientCache>>(mapOf())
    val serverFTPMap = _serverFTPMap

    private val _downloadFTPSet = MutableStateFlow<MutableSet<SingleFTPClient>>(mutableSetOf())
    val downloadFTPList = _downloadFTPSet.asStateFlow()

    private val _uploadFTPSet = MutableStateFlow<MutableSet<SingleFTPClient>>(mutableSetOf())
    val uploadFTPList = _uploadFTPSet.asStateFlow()

    fun getAllCache(): List<FTPClientCache> {
        return _serverFTPMap.value.values.toList()
    }

    fun getCacheByHost(host: String): FTPClientCache? {
        return _serverFTPMap.value[host]
    }

    fun putCacheByHost(host: String, ftpClient: SingleFTPClient) {
        _serverFTPMap.update { map ->
            map.toMutableMap().apply {
                put(host, FTPClientCache(ftpClient))
                println("Put Cache ByHost")
            }
        }
    }

    fun addToDownloadList(singleFTPClient: SingleFTPClient) {
        _downloadFTPSet.value = _downloadFTPSet.value.toMutableSet().apply { add(singleFTPClient) }

    }

    fun removeFromDownloadList(singleFTPClient: SingleFTPClient) {
        _serverFTPMap.value[singleFTPClient.host]?.idleClientFromDownload(singleFTPClient)
        _downloadFTPSet.value =
            _downloadFTPSet.value.toMutableSet().apply { remove(singleFTPClient) }
    }

    fun addToUploadList(singleFTPClient: SingleFTPClient) {
        _uploadFTPSet.value = _uploadFTPSet.value.toMutableSet().apply { add(singleFTPClient) }
    }

    fun removeFromUploadList(singleFTPClient: SingleFTPClient) {
        _serverFTPMap.value[singleFTPClient.host]?.idleClientFromUpload(singleFTPClient)
        _uploadFTPSet.value =
            _uploadFTPSet.value.toMutableSet().apply { remove(singleFTPClient) }
    }


}