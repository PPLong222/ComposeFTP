package indi.pplong.composelearning.core.host.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.cache.FTPServerPool
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.model.toItemInfo
import indi.pplong.composelearning.core.host.repo.ServerItemRepository
import indi.pplong.composelearning.ftp.SingleFTPClient
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 2:38 PM
 */
@HiltViewModel
class HostsViewModel @Inject constructor(
    private val serverItemRepository: ServerItemRepository,
    private val transferredFileDao: TransferredFileDao
) : BaseViewModel<ServerUiState, ServerUiIntent, ServerUiEffect>() {

    init {
        getServerList()
    }

    private fun getServerList() {
        launchOnIO {
            setState { copy(listLoadingState = LoadingState.LOADING) }
            val list = serverItemRepository.getAllItems().map { it.toItemInfo() }
            setState { copy(serverList = list, listLoadingState = LoadingState.SUCCESS) }
        }
    }

    override fun initialState(): ServerUiState {
        return ServerUiState()
    }

    override suspend fun handleIntent(intent: ServerUiIntent) {
        when (intent) {
            is ServerUiIntent.ConnectServer -> {
                connect(intent.serverItemInfo)
            }

            is ServerUiIntent.OnServerEdited -> {
                getServerList()
            }
        }
    }

    private fun handleNavigate() {
        viewModelScope.launch {
            sendEffect {
                ServerUiEffect.NavigateToFilePage
            }
        }
    }

    private fun connect(serverItemInfo: ServerItemInfo) {
        setState { copy(connectedState = LoadingState.LOADING, connectedServer = serverItemInfo) }
        if (FTPServerPool.getCacheByHost(serverItemInfo.host) != null) {
            setState {
                copy(
                    connectedState = LoadingState.SUCCESS,
                    connectedServer = serverItemInfo
                )
            }
            handleNavigate()
        }
        if (FTPServerPool.getCacheByHost(serverItemInfo.host) == null) {
            launchOnIO {
                serverItemInfo.apply {
                    val client = SingleFTPClient(host, port, user, password, transferredFileDao)
                    client.initClient(
                        onSuccess = {
                            FTPServerPool.putCacheByHost(host, client)
                            setState {
                                copy(
                                    connectedState = LoadingState.SUCCESS,
                                    connectedServer = serverItemInfo
                                )
                            }
                            handleNavigate()
                        },
                        onFail = {
                            setState { copy(connectedState = LoadingState.FAIL) }
                        }
                    )
                }
            }
        }
    }
}