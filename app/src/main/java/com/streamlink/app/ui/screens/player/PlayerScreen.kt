package com.streamlink.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun PlayerScreen(
    mediaItemId: String,
    episodeId: String?,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer view will go here — Phase 2
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "נגן וידאו\n(Phase 2)",
                style = MaterialTheme.typography.titleMedium,
                color = StreamLinkColors.TextSecondary
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "חזור", tint = Color.White)
        }
    }
}
