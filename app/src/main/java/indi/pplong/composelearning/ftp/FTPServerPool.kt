package indi.pplong.composelearning.ftp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.ftp.ftp.BaseFTPClient
import indi.pplong.composelearning.ftp.sftp.SFTPBaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val _serverFTPMap = MutableStateFlow<Map<Long, FTPClientCache>>(mapOf())
    val serverFTPMap = _serverFTPMap

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadFTPSet = _serverFTPMap.flatMapLatest { map ->
        if (map.isEmpty()) {
            flowOf(emptySet())
        } else {
            val flows = map.values.map { it.ftpClientState.map { it.downloadList } }
            combine(flows) { sets ->
                sets.flatMap { it }.toSet()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pausedTransferringFile = _serverFTPMap.flatMapLatest { map ->
        if (map.isEmpty()) {
            flowOf(emptyList())
        } else {
            val flows = map.values.map { it.ftpClientState.map { it.pausedList } }
            combine(flows) { sets ->
                sets.flatMap { it }.toList()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uploadFTPSet = _serverFTPMap.flatMapLatest { map ->
        if (map.isEmpty()) {
            flowOf(emptySet())
        } else {
            val flows = map.values.map { it.ftpClientState.map { it.uploadList } }
            combine(flows) { sets ->
                sets.flatMap { it }.toSet()
            }
        }
    }

    fun getCacheByHost(hostKey: Long): FTPClientCache? {
        return _serverFTPMap.value[hostKey]
    }

    suspend fun initNewCache(
        config: FTPConfig
    ): Boolean {

        val ftpClientCache = FTPClientCache(config, context, transferredFileDao)

        if (ftpClientCache.coreFTPClient.initClient() && ftpClientCache.thumbnailFTPClient.initClient()) {
            _serverFTPMap.update { map ->
                map.toMutableMap().apply {
                    put(config.key, ftpClientCache)
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