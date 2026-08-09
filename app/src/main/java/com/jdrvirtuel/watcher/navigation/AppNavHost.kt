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
import androidx.navigation.navDeepLink
import com.jdrvirtuel.watcher.feature.diagnostic.DiagnosticScreen
import com.jdrvirtuel.watcher.feature.debug.DebugScreen
import com.jdrvirtuel.watcher.feature.forumdetail.ForumDetailScreen
import com.jdrvirtuel.watcher.feature.forumdetail.ForumDetailViewModel
import com.jdrvirtuel.watcher.feature.home.HomeScreen
import com.jdrvirtuel.watcher.feature.settings.SettingsScreen
import com.jdrvirtuel.watcher.feature.verification.VerificationScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = HomeRoute,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToForum = { forumId -> navController.navigate(ForumDetailRoute(forumId)) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToVerification = { navController.navigate(VerificationRoute) }
            )
        }
        composable<DiagnosticRoute> {
            DiagnosticScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(DiagnosticRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDebug = { navController.navigate(DebugRoute) },
                onNavigateToDiagnostic = { navController.navigate(DiagnosticRoute) }
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
        composable<VerificationRoute>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "jdrvirtuel://verification" }
            )
        ) {
            VerificationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
