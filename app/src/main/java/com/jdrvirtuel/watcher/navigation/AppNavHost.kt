package com.jdrvirtuel.watcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jdrvirtuel.watcher.feature.debug.DebugScreen
import com.jdrvirtuel.watcher.feature.home.HomeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToDebug = { navController.navigate(DebugRoute) }
            )
        }
        composable<DebugRoute> {
            DebugScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
