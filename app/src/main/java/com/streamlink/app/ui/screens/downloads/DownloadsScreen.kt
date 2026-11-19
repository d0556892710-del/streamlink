package com.streamlink.app.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun DownloadsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            "הורדות",
            style = MaterialTheme.typography.headlineMedium,
            color = StreamLinkColors.TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "אין הורדות פעילות",
                    style = MaterialTheme.typography.titleMedium,
                    color = StreamLinkColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "קבצים שמורדים יופיעו כאן",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StreamLinkColors.TextTertiary
                )
            }
        }
    }
}
