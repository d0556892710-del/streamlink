package com.streamlink.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.streamlink.app.ui.screens.downloads.DownloadsScreen
import com.streamlink.app.ui.screens.home.HomeScreen
import com.streamlink.app.ui.screens.library.LibraryScreen
import com.streamlink.app.ui.screens.settings.SettingsScreen
import com.streamlink.app.ui.screens.sources.SourcesScreen
import com.streamlink.app.ui.screens.details.SeriesDetailsScreen
import com.streamlink.app.ui.screens.player.PlayerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Sources : Screen("sources")
    object Settings : Screen("settings")
    object SeriesDetails : Screen("series/{seriesId}") {
        fun createRoute(seriesId: String) = "series/$seriesId"
    }
    object Player : Screen("player/{mediaItemId}?episodeId={episodeId}") {
        fun createRoute(mediaItemId: String, episodeId: String? = null): String {
            return if (episodeId != null) {
                "player/$mediaItemId?episodeId=$episodeId"
            } else {
                "player/$mediaItemId"
            }
        }
    }
}

@Composable
fun StreamLinkNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetails = { seriesId ->
                    navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                },
                onNavigateToPlayer = { mediaItemId ->
                    navController.navigate(Screen.Player.createRoute(mediaItemId))
                }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToDetails = { seriesId ->
                    navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                },
                onNavigateToPlayer = { mediaItemId ->
                    navController.navigate(Screen.Player.createRoute(mediaItemId))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen()
        }

        composable(Screen.Sources.route) {
            SourcesScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.SeriesDetails.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            SeriesDetailsScreen(
                seriesId = seriesId,
                onNavigateToPlayer = { mediaItemId, episodeId ->
                    navController.navigate(Screen.Player.createRoute(mediaItemId, episodeId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("mediaItemId") { type = NavType.StringType },
                navArgument("episodeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val mediaItemId = backStackEntry.arguments?.getString("mediaItemId") ?: return@composable
            val episodeId = backStackEntry.arguments?.getString("episodeId")
            PlayerScreen(
                mediaItemId = mediaItemId,
                episodeId = episodeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
