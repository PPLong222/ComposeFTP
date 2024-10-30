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
import indi.pplong.composelearning.core.host.viewmodel.HostsViewModel

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 8:32 PM
 */

@Preview
@Composable
fun HostsList(viewModel: HostsViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        if (uiState.value.serverList.isEmpty()) {
            item {
                Text("No Available Host")
            }
        } else {
            items(uiState.value.serverList) {
                ConnectedHost(
                    it,
                    viewModel::sendIntent,
                    (uiState.value.connectedState == LoadingState.LOADING && it == uiState.value.connectedServer)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}