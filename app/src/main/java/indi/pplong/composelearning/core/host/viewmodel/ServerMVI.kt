package indi.pplong.composelearning.core.host.viewmodel

import indi.pplong.composelearning.core.base.mvi.UiEffect
import indi.pplong.composelearning.core.base.mvi.UiIntent
import indi.pplong.composelearning.core.base.mvi.UiState
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.host.model.ServerItemInfo

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 2:45 PM
 */
data class ServerUiState(
    val serverList: List<ServerItemInfo> = arrayListOf(),
    val listLoadingState: LoadingState? = null,
    val connectedState: LoadingState? = null,
    val connectedServer: ServerItemInfo? = null,
) : UiState

sealed class ServerUiIntent : UiIntent {
    data class ConnectServer(val serverItemInfo: ServerItemInfo) : ServerUiIntent()
    data object OnServerEdited : ServerUiIntent()
}

sealed class ServerUiEffect : UiEffect {
    data object NavigateToFilePage : ServerUiEffect()
}

