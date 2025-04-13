package indi.pplong.composelearning.core.host.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.ConfigureState
import indi.pplong.composelearning.core.base.state.EditState
import indi.pplong.composelearning.core.host.model.ConnectivityTestState
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.model.toFTPConfig
import indi.pplong.composelearning.core.host.model.toItem
import indi.pplong.composelearning.core.host.repo.ServerItemRepository
import org.apache.commons.net.ftp.FTP
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 11:04 PM
 */
@HiltViewModel
class EditServerViewModel @Inject constructor(
    private val globalViewModel: GlobalRepository,
    private val serverItemRepository: ServerItemRepository
) : BaseViewModel<EditServerUiState, EditServerIntent, EditServerEffect>() {
    override fun initialState(): EditServerUiState {
        return EditServerUiState(
            host = ServerItemInfo(
                lastConnectedTime = System.currentTimeMillis(),
                port = FTP.DEFAULT_PORT
            )
        )
    }

    private fun testConnectivity() {
        setState { copy(state = ConnectivityTestState.TESTING) }
        launchOnIO {
            val isSuccess =
                globalViewModel.pool.testHostServerConnectivity(
                    uiState.value.host.toFTPConfig()
                )
            setState { copy(state = if (isSuccess) ConnectivityTestState.SUCCESS else ConnectivityTestState.FAIL) }
        }
    }

    override suspend fun handleIntent(intent: EditServerIntent) {
        when (intent) {
            is EditServerIntent.TestConnectivity -> testConnectivity()
            is EditServerIntent.SaveHost -> saveHost()
            is EditServerIntent.OnChangeHostInfo -> changeAndCheckHostInfo(intent.host)
            is EditServerIntent.OnDismiss -> onDismiss(intent.success)
            EditServerIntent.NextToConfigure -> nextToConfigure()
        }
    }

    private fun nextToConfigure() {
        setState {
            copy(
                configureState = ConfigureState.CONFIGURING,
                host = host.copy(nickname = host.host)
            )
        }
    }

    private fun onDismiss(success: Boolean) {
        if (success) {
            setState { initialState() }
        }
    }

    private fun saveHost() {
        launchOnIO {
            // TODO: Primary Key Error Tips
            serverItemRepository.insert(uiState.value.host.toItem())
            setState {
                copy(editState = EditState.SUCCESS)
            }
        }
    }

    private fun changeAndCheckHostInfo(host: ServerItemInfo) {
        val hostCopy =
            if (host.isSFTP && host.port == FTP.DEFAULT_PORT) {
                host.copy(port = 22)
            } else if (!host.isSFTP && host.port == 22) {
                host.copy(port = FTP.DEFAULT_PORT)
            } else {
                host.copy()
            }
        setState { copy(host = hostCopy, state = ConnectivityTestState.INITIAL) }
    }
}