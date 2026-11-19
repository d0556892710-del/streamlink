package com.streamlink.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun LibraryScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("הכל") }
    val categories = listOf("הכל", "סרטים", "סדרות", "הורדות", "לא הושלם")

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
                "ספרייה",
                style = MaterialTheme.typography.headlineMedium,
                color = StreamLinkColors.TextPrimary
            )
            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, contentDescription = "חיפוש", tint = StreamLinkColors.TextPrimary)
                }
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "שנה תצוגה",
                        tint = StreamLinkColors.TextPrimary
                    )
                }
            }
        }

        // Category chips
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory),
            containerColor = StreamLinkColors.Background,
            contentColor = StreamLinkColors.Accent,
            edgePadding = 16.dp,
            divider = {}
        ) {
            categories.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            category,
                            color = if (selectedCategory == category)
                                StreamLinkColors.Accent
                            else
                                StreamLinkColors.TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "הספרייה ריקה",
                    style = MaterialTheme.typography.titleMedium,
                    color = StreamLinkColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "הוסף מקור Telegram כדי להתחיל",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StreamLinkColors.TextTertiary
                )
            }
        }
    }
}
