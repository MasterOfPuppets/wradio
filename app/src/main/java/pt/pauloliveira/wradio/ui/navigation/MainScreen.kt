package pt.pauloliveira.wradio.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.pauloliveira.wradio.ui.home.HomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playerState.errorMsg) {
        playerState.errorMsg?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true
            )
            viewModel.onErrorShown()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Explore,
        Screen.MyRadio,
        Screen.Management,
        Screen.Settings
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val currentScreen = items.find { it.route == currentDestination?.route }
                ?: Screen.MyRadio

            CenterAlignedTopAppBar(
                title = { Text(stringResource(currentScreen.title)) }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playerState.station != null) {
                    MiniPlayer(
                        station = playerState.station!!,
                        isPlaying = playerState.isPlaying,
                        isBuffering = playerState.isBuffering,
                        metadata = playerState.metadata,
                        onPlayPauseClick = { viewModel.togglePlayPause() }
                    )
                }

                NavigationBar {
                    items.forEach { screen ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.title)) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.MyRadio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Explore.route) {
                pt.pauloliveira.wradio.ui.explore.ExploreScreen()
            }
            composable(Screen.MyRadio.route) {
                HomeScreen()
            }
            composable(Screen.Management.route) {
                pt.pauloliveira.wradio.ui.management.ManagementScreen()
            }
            composable(Screen.Settings.route) {
                pt.pauloliveira.wradio.ui.settings.SettingsScreen()
            }
        }
    }
}