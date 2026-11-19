package com.streamlink.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    onNavigateToPlayer: (mediaItemId: String, episodeId: String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Hero image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(StreamLinkColors.Surface)
                )
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("כותרת הסדרה", style = MaterialTheme.typography.displayMedium, color = StreamLinkColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("תיאור הסדרה...", style = MaterialTheme.typography.bodyMedium, color = StreamLinkColors.TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = StreamLinkColors.Accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶  המשך צפייה", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }

        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "חזור", tint = androidx.compose.ui.graphics.Color.White)
        }
    }
}
