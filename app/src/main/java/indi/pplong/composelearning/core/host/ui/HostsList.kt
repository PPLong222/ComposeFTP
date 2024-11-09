package indi.pplong.composelearning.core.host.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.viewmodel.HostsViewModel
import indi.pplong.composelearning.core.host.viewmodel.ServerUiIntent
import indi.pplong.composelearning.core.host.viewmodel.ServerUiState
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 8:32 PM
 */

@Composable
fun HostsList(
    uiState: ServerUiState,
    onIntent: (ServerUiIntent) -> Unit,
) {
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        if (uiState.serverList.isEmpty()) {
            item {
                Text("No Available Host")
            }
        } else {
            items(uiState.serverList) {
                ConnectedHost(
                    serverItemInfo = it,
                    onIntent = onIntent,
                    isConnecting =
                        uiState.connectedState == LoadingState.LOADING && it == uiState.connectedServer
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Preview
@Composable
fun HostsListPreview() {
    ComposeLearningTheme {
        HostsList(
            uiState = ServerUiState(),
            onIntent = {}
        )
    }
}