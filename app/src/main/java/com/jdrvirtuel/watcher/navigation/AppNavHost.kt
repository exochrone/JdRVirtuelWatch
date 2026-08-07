package com.jdrvirtuel.watcher.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jdrvirtuel.watcher.feature.debug.DebugScreen
import com.jdrvirtuel.watcher.feature.forumdetail.ForumDetailScreen
import com.jdrvirtuel.watcher.feature.forumdetail.ForumDetailViewModel
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
                onNavigateToForum = { forumId -> navController.navigate(ForumDetailRoute(forumId)) },
                onNavigateToDebug = { navController.navigate(DebugRoute) }
            )
        }
        composable<DebugRoute> {
            DebugScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ForumDetailRoute> {
            val viewModel: ForumDetailViewModel = hiltViewModel()
            ForumDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
