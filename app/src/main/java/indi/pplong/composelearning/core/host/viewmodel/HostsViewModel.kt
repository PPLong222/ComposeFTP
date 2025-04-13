package indi.pplong.composelearning.core.host.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.cache.thumbnail.ThumbnailCacheDao
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.host.model.ServerConnectionStatus
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.model.toFTPConfig
import indi.pplong.composelearning.core.host.model.toItemInfo
import indi.pplong.composelearning.core.host.repo.ServerItemRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 2:38 PM
 */
@HiltViewModel
class HostsViewModel @Inject constructor(
    private val globalViewModel: GlobalRepository,
    private val serverItemRepository: ServerItemRepository,
    private val transferredFileDao: TransferredFileDao,
    private val thumbnailCacheDao: ThumbnailCacheDao
) : BaseViewModel<ServerUiState, ServerUiIntent, ServerUiEffect>() {

    init {
        getServerList()
    }

    private fun getServerList() {
        launchOnIO {
            setState { copy(listLoadingState = LoadingState.LOADING) }
            val list = serverItemRepository.getAllItems().map { it.toItemInfo() }.toMutableList()
            // TODO: DELETE Mock Data
            repeat(10) {
                list.add(ServerItemInfo())
            }
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

    private fun handleNavigate(host: String) {
        viewModelScope.launch {
            sendEffect {
                ServerUiEffect.NavigateToFilePage(host)
            }
        }
    }

    private fun connect(serverItemInfo: ServerItemInfo) {
        setState {
            copy(serverList = serverList.map {
                if (it == serverItemInfo) {
                    it.copy(connectedStatus = ServerConnectionStatus.CONNECTING)
                } else {
                    it
                }
            })
        }
        if (globalViewModel.pool.getCacheByHost(serverItemInfo.host) != null) {
            setState {
                copy(serverList = serverList.map {
                    if (it == serverItemInfo) {
                        it.copy(connectedStatus = ServerConnectionStatus.CONNECTED)
                    } else {
                        it
                    }
                })
            }
            handleNavigate(serverItemInfo.host)
        } else {
            launchOnIO {
                serverItemInfo.apply {
                    val res = globalViewModel.pool.initNewCache(
                        serverItemInfo.toFTPConfig()
                    )

                    if (res) {
                        setState {
                            copy(serverList = serverList.map {
                                if (it == serverItemInfo) {
                                    it.copy(connectedStatus = ServerConnectionStatus.CONNECTED)
                                } else {
                                    it
                                }
                            })
                        }
                        handleNavigate(serverItemInfo.host)
                    } else {
                        setState {
                            copy(serverList = serverList.map {
                                if (it == serverItemInfo) {
                                    it.copy(connectedStatus = ServerConnectionStatus.FAILED)
                                } else {
                                    it
                                }
                            })
                        }

                    }
                }
            }
        }
    }
}