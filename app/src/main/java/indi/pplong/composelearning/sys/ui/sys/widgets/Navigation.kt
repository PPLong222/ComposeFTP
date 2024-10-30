package indi.pplong.composelearning.sys.ui.sys.widgets

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import indi.pplong.composelearning.core.file.ui.BrowsePage
import indi.pplong.composelearning.core.host.ui.HostPage
import indi.pplong.composelearning.core.host.viewmodel.HostsViewModel
import indi.pplong.composelearning.core.load.ui.TransferScreen

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 3:28 PM
 */

enum class BasicBottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    Hosts("host", Icons.Filled.Home, "Host"),
    Server(
        "Browse", Icons.Default.Menu, "Browse"
    ),
    Download("transfer", Icons.Default.KeyboardArrowUp, "Transfer"),
}

@Composable
fun CommonBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        BasicBottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any {
                    Log.d("123123", "CommonBottomNavigationBar: ${it.route} ${item.route}")
                    it.route == item.route
                } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
fun CommonNavigationHost(navController: NavHostController, modifier: Modifier) {
    val mainViewModel: HostsViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    NavHost(
        navController = navController,
        startDestination = BasicBottomNavItem.Hosts.route,
        modifier = modifier
    ) {
        composable(BasicBottomNavItem.Hosts.route) { HostPage(navController, mainViewModel) }
        composable(BasicBottomNavItem.Server.route) {
            BrowsePage(
                uiState.connectedServer?.host ?: ""
            )
        }
        composable(BasicBottomNavItem.Download.route) { TransferScreen() }
    }
}