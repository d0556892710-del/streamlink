package com.streamlink.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streamlink.app.ui.navigation.*
import com.streamlink.app.ui.theme.StreamLinkColors
import com.streamlink.app.ui.theme.StreamLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StreamLinkTheme {
                StreamLinkApp()
            }
        }
    }
}

@Composable
fun StreamLinkApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom nav during video playback
    val showBottomNav = currentRoute != Screen.Player.route &&
            !currentRoute.orEmpty().startsWith("player/")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background),
        containerColor = StreamLinkColors.Background,
        bottomBar = {
            StreamLinkBottomNav(
                navController = navController,
                visible = showBottomNav
            )
        }
    ) { paddingValues ->
        StreamLinkNavGraph(
            navController = navController,
            modifier = Modifier.padding(
                bottom = if (showBottomNav) paddingValues.calculateBottomPadding() else 0.dp
            )
        )
    }
}
