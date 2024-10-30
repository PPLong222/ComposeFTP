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
import indi.pplong.composelearning.core.host.viewmodel.EditServerViewModel
import indi.pplong.composelearning.core.host.viewmodel.HostsViewModel
import indi.pplong.composelearning.core.host.viewmodel.ServerUiEffect
import indi.pplong.composelearning.core.host.viewmodel.ServerUiIntent
import indi.pplong.composelearning.sys.ui.sys.widgets.BasicBottomNavItem

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 3:49 PM
 */

@Preview
@Composable
fun HostPage(
    navController: NavHostController = rememberNavController(),
    mainViewModel: HostsViewModel = hiltViewModel()
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val viewModel: EditServerViewModel = hiltViewModel()
    val editServerUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        mainViewModel.uiEffect.collect { effect ->
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
            HostsList(mainViewModel)
        }

        if (showBottomSheet) {
            EditServerBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.sendIntent(EditServerIntent.OnDismiss(true))
                    if (editServerUiState.editState == EditState.SUCCESS) {
                        mainViewModel.sendIntent(ServerUiIntent.OnServerEdited)
                    }
                },
                uiState = editServerUiState,
                onIntent = viewModel::sendIntent
            )
        }
    }

}