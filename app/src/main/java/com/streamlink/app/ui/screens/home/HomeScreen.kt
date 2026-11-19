package com.streamlink.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun HomeScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Hero section — will show last watched series
                HeroSection(
                    onPlayClick = { /* TODO */ },
                    onMoreInfoClick = { /* TODO */ }
                )
            }

            item {
                SectionHeader(title = "המשך צפייה")
                ContinueWatchingRow(onItemClick = onNavigateToPlayer)
            }

            item {
                SectionHeader(title = "נוספו לאחרונה")
                MediaRow(onItemClick = { onNavigateToDetails(it) })
            }

            item {
                SectionHeader(title = "הסדרות שלי")
                MediaRow(onItemClick = { onNavigateToDetails(it) })
            }

            item {
                SectionHeader(title = "הסרטים שלי")
                MediaRow(onItemClick = { onNavigateToDetails(it) })
            }
        }
    }
}

@Composable
private fun HeroSection(
    onPlayClick: () -> Unit,
    onMoreInfoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(StreamLinkColors.BackgroundSecondary)
    ) {
        // Artwork placeholder — will be replaced with real Coil image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1D26),
                            StreamLinkColors.Background
                        )
                    )
                )
        )

        // Bottom gradient + buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, StreamLinkColors.Background)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "הוסף מקור Telegram כדי להתחיל",
                    style = MaterialTheme.typography.displayMedium,
                    color = StreamLinkColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "הספרייה שלך תופיע כאן לאחר הוספת תוכן",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StreamLinkColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamLinkColors.Accent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("▶  נגן", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onMoreInfoClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StreamLinkColors.TextPrimary
                        )
                    ) {
                        Text("מידע נוסף")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = StreamLinkColors.TextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ContinueWatchingRow(onItemClick: (String) -> Unit) {
    // Placeholder — real data from ViewModel
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp)
            .background(StreamLinkColors.Surface, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "לא נצפה עדיין",
            style = MaterialTheme.typography.bodyMedium,
            color = StreamLinkColors.TextTertiary
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MediaRow(onItemClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) { // placeholder items
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .background(StreamLinkColors.Surface, RoundedCornerShape(10.dp))
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
