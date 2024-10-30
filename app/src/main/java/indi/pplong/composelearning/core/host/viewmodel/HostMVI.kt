package indi.pplong.composelearning.core.host.viewmodel

import indi.pplong.composelearning.core.base.mvi.UiEffect
import indi.pplong.composelearning.core.base.mvi.UiIntent
import indi.pplong.composelearning.core.base.mvi.UiState
import indi.pplong.composelearning.core.base.state.ConfigureState
import indi.pplong.composelearning.core.base.state.EditState
import indi.pplong.composelearning.core.host.model.ConnectivityTestState
import indi.pplong.composelearning.core.host.model.ServerItemInfo

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 10:52 PM
 */
data class EditServerUiState(
    val host: ServerItemInfo = ServerItemInfo(),
    val state: ConnectivityTestState = ConnectivityTestState.INITIAL,
    val configureState: ConfigureState = ConfigureState.CONNECTING,
    val editState: EditState = EditState.INITIAL
) : UiState

sealed class EditServerIntent : UiIntent {
    data object TestConnectivity : EditServerIntent()
    data object SaveHost : EditServerIntent()
    data object NextToConfigure : EditServerIntent()
    data class OnChangeHostInfo(val host: ServerItemInfo) : EditServerIntent()
    data class OnDismiss(val success: Boolean) : EditServerIntent()
}

sealed class EditServerEffect : UiEffect {

}