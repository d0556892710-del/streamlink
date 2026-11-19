package com.streamlink.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.streamlink.app.R
import com.streamlink.app.ui.theme.StreamLinkColors

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    BottomNavItem(Screen.Library.route, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, R.string.nav_library),
    BottomNavItem(Screen.Downloads.route, Icons.Filled.Download, Icons.Outlined.Download, R.string.nav_downloads),
    BottomNavItem(Screen.Sources.route, Icons.Filled.Source, Icons.Outlined.Source, R.string.nav_sources),
    BottomNavItem(Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.nav_settings),
)

@Composable
fun StreamLinkBottomNav(
    navController: NavController,
    visible: Boolean = true
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, StreamLinkColors.Background)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    BottomNavItemView(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(
                    if (isSelected) Modifier
                        .clip(RoundedCornerShape(50))
                        .background(StreamLinkColors.Accent.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                    else Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = stringResource(item.labelRes),
                tint = if (isSelected) StreamLinkColors.Accent else StreamLinkColors.TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(item.labelRes),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = if (isSelected) StreamLinkColors.Accent else StreamLinkColors.TextTertiary
        )
    }
}
