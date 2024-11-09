package indi.pplong.composelearning.core.host.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import indi.pplong.composelearning.core.base.state.EditState
import indi.pplong.composelearning.core.host.viewmodel.EditServerIntent
import indi.pplong.composelearning.core.host.viewmodel.EditServerUiState
import indi.pplong.composelearning.core.host.viewmodel.EditServerViewModel
import indi.pplong.composelearning.core.host.viewmodel.HostsViewModel
import indi.pplong.composelearning.core.host.viewmodel.ServerUiEffect
import indi.pplong.composelearning.core.host.viewmodel.ServerUiIntent
import indi.pplong.composelearning.core.host.viewmodel.ServerUiState
import indi.pplong.composelearning.sys.ui.sys.widgets.BasicBottomNavItem
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 3:49 PM
 */

@Composable
fun HostPageRoute(
    navController: NavHostController = rememberNavController(),
    hostViewModel: HostsViewModel = hiltViewModel(),
    editServerViewModel: EditServerViewModel = hiltViewModel()
) {
    val serverUiState by hostViewModel.uiState.collectAsStateWithLifecycle()
    val editServerUiState by editServerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        hostViewModel.uiEffect.collect { effect ->
            when (effect) {
                is ServerUiEffect.NavigateToFilePage -> {
                    navController.navigate(BasicBottomNavItem.Server.route) {
                        // Why doing this?
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    HostPage(
        serverUiState = serverUiState,
        editServerUiState = editServerUiState,
        onServerIntent = hostViewModel::sendIntent,
        onEditServerIntent = editServerViewModel::sendIntent
    )
}

@Composable
fun HostPage(
    serverUiState: ServerUiState,
    editServerUiState: EditServerUiState,
    onServerIntent: (ServerUiIntent) -> Unit,
    onEditServerIntent: (EditServerIntent) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = showBottomSheet.not() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, null)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Spacer(Modifier.padding(innerPadding))
        Column {
            Text("Hosts", style = MaterialTheme.typography.headlineLarge)
            HostsList(
                uiState = serverUiState,
                onIntent = onServerIntent
            )
        }

        if (showBottomSheet) {
            EditServerBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    onEditServerIntent(EditServerIntent.OnDismiss(true))
                    if (editServerUiState.editState == EditState.SUCCESS) {
                        onServerIntent(ServerUiIntent.OnServerEdited)
                    }
                },
                uiState = editServerUiState,
                onIntent = onEditServerIntent
            )
        }
    }
}

@Preview
@Composable
fun HostPagePreview() {
    ComposeLearningTheme {
        HostPage(
            serverUiState = ServerUiState(),
            editServerUiState = EditServerUiState(),
            onServerIntent = {},
            onEditServerIntent = {}
        )
    }
}