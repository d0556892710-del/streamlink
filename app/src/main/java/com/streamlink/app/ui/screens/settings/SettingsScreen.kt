package com.streamlink.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.streamlink.app.ui.theme.StreamLinkColors

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamLinkColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            "הגדרות",
            style = MaterialTheme.typography.headlineMedium,
            color = StreamLinkColors.TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { SettingsSection("חשבון") }
            item { SettingsItem(Icons.Default.Person, "פרופיל", "ניהול חשבון") }
            item { SettingsItem(Icons.Default.CloudSync, "ענן וסנכרון", "גיבוי וסנכרון מכשירים") }

            item { SettingsSection("Telegram") }
            item { SettingsItem(Icons.Default.Send, "חיבורי Telegram", "ניהול מקורות מחוברים") }

            item { SettingsSection("הורדות ואחסון") }
            item { SettingsItem(Icons.Default.Download, "הורדות", "אחסון מקומי ואיכות") }
            item { SettingsItem(Icons.Default.Wifi, "Wi-Fi בלבד", "הורד רק ב-Wi-Fi") }
            item { SettingsItem(Icons.Default.Storage, "אחסון", "נהל מקום בדיסק") }

            item { SettingsSection("ניגון") }
            item { SettingsItem(Icons.Default.PlayArrow, "העדפות ניגון", "מהירות, איכות, כתוביות") }
            item { SettingsItem(Icons.Default.Subtitles, "כתוביות", "גופן, גודל, צבע") }

            item { SettingsSection("מראה") }
            item { SettingsItem(Icons.Default.Language, "שפה", "עברית (RTL)") }

            item { SettingsSection("פרטיות ואבטחה") }
            item { SettingsItem(Icons.Default.Security, "פרטיות", "הרשאות ואנליטיקס") }
            item { SettingsItem(Icons.Default.DeleteSweep, "נקה מטמון", "פנה שטח אחסון") }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "התנתק מ-Telegram",
                    subtitle = "נתק את חשבון Telegram",
                    tint = StreamLinkColors.Warning
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "מחק חשבון",
                    subtitle = "מחק את כל הנתונים",
                    tint = StreamLinkColors.Error
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "StreamLink v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = StreamLinkColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = StreamLinkColors.Accent,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color = StreamLinkColors.TextPrimary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StreamLinkColors.TextTertiary)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = StreamLinkColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = StreamLinkColors.Divider,
        thickness = 0.5.dp
    )
}
