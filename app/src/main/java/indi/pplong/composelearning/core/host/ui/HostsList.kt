package indi.pplong.composelearning.core.host.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
    if (uiState.serverList.isEmpty()) {
        Text("No Available Host")
    } else {
        StackedHostCardPager(
            uiState = uiState,
            onIntent = onIntent
        )
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