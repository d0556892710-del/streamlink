package com.streamlink.app.ui.screens.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun SourcesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "מקורות",
                style = MaterialTheme.typography.headlineMedium,
                color = StreamLinkColors.TextPrimary
            )
            FloatingActionButton(
                onClick = { /* TODO: Add source flow */ },
                containerColor = StreamLinkColors.Accent,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף מקור", tint = androidx.compose.ui.graphics.Color.White)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "לא חובר מקור",
                    style = MaterialTheme.typography.titleMedium,
                    color = StreamLinkColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "לחץ + כדי להוסיף בוט Telegram",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StreamLinkColors.TextTertiary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(containerColor = StreamLinkColors.Accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("הוסף מקור Telegram", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
