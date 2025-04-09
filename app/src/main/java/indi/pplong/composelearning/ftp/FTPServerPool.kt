package indi.pplong.composelearning.ftp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.ftp.clients.CoreFTPClient
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
    private val ftpClientCacheFactory: FTPClientCache.Factory
) {
    private val _serverFTPMap = MutableStateFlow<Map<String, FTPClientCache>>(mapOf())
    val serverFTPMap = _serverFTPMap

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadFTPSet = _serverFTPMap.map { map ->
        map.values.map { it.downloadQueue }
    }.flatMapLatest {
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

    fun getAllCache(): List<FTPClientCache> {
        return _serverFTPMap.value.values.toList()
    }

    fun getCacheByHost(host: String): FTPClientCache? {
        return _serverFTPMap.value[host]
    }

    fun putCacheByHost(host: String, ftpClient: CoreFTPClient) {
        _serverFTPMap.update { map ->
            map.toMutableMap().apply {
                put(host, ftpClientCacheFactory.create(ftpClient))
                println("Put Cache ByHost")
            }
        }
    }

    fun initNewCache(
        host: String,
        port: Int,
        user: String,
        password: String
    ): Boolean {
        val createNewClient =
            createNewClient(host, port, user, password)
        val ftpClientCache = ftpClientCacheFactory.create(createNewClient)
        if (createNewClient.initClient() && ftpClientCache.thumbnailFTPClient.initClient()) {
            _serverFTPMap.update { map ->
                map.toMutableMap().apply {
                    put(host, ftpClientCache)
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
        password: String
    ): CoreFTPClient {
        return CoreFTPClient(
            host,
            port,
            user,
            password,
            context
        )
    }


    suspend fun testHostServerConnectivity(
        host: String,
        passwd: String,
        user: String,
        port: Int,
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