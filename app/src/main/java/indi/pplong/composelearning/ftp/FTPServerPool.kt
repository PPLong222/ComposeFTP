package indi.pplong.composelearning.ftp

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.ftp.ftp.BaseFTPClient
import indi.pplong.composelearning.ftp.sftp.SFTPBaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:52 PM
 */
@Singleton
class FTPServerPool @Inject constructor(
    @ApplicationContext val context: Context,
    val transferredFileDao: TransferredFileDao
) {
    private val _serverFTPMap = MutableStateFlow<Map<String, FTPClientCache>>(mapOf())
    val serverFTPMap = _serverFTPMap

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadFTPSet = _serverFTPMap.map { map ->
        Log.d("TTTest", "Client: Map changed")
        map.values.map { it.downloadQueue }
    }.flatMapLatest {
        Log.d("TTTest", "Client")

        combine(it) { sets ->
            sets.flatMap { it }.toSet()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uploadFTPSet = _serverFTPMap.map { map ->
        map.values.map { it.uploadQueue }
    }.flatMapLatest {
        combine(it) { sets ->
            sets.flatMap { it }.toSet()
        }
    }

    fun getCacheByHost(host: String): FTPClientCache? {
        return _serverFTPMap.value[host]
    }

    suspend fun initNewCache(
        config: FTPConfig
    ): Boolean {

        val ftpClientCache = FTPClientCache(config, context, transferredFileDao)
        if (ftpClientCache.coreFTPClient.initClient() && ftpClientCache.thumbnailFTPClient.initClient()) {
            _serverFTPMap.update { map ->
                map.toMutableMap().apply {
                    put(config.host, ftpClientCache)
                }
            }
            return true
        }
        return false
    }

    suspend fun testHostServerConnectivity(
        config: FTPConfig
    ): Boolean {
        val ftpClient =
            if (config.isSFTP) {
                SFTPBaseClient(config)
            } else {
                BaseFTPClient(config)
            }
        val res = ftpClient.initClient()
        ftpClient.close()
        return res
    }
}