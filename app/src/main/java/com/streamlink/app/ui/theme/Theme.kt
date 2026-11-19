package com.streamlink.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StreamLinkDarkColorScheme = darkColorScheme(
    primary = StreamLinkColors.Accent,
    onPrimary = Color.White,
    primaryContainer = StreamLinkColors.AccentDark,
    onPrimaryContainer = Color.White,

    secondary = StreamLinkColors.SurfaceVariant,
    onSecondary = StreamLinkColors.TextPrimary,
    secondaryContainer = StreamLinkColors.Surface,
    onSecondaryContainer = StreamLinkColors.TextPrimary,

    tertiary = StreamLinkColors.TextSecondary,
    onTertiary = StreamLinkColors.Background,

    background = StreamLinkColors.Background,
    onBackground = StreamLinkColors.TextPrimary,

    surface = StreamLinkColors.BackgroundSecondary,
    onSurface = StreamLinkColors.TextPrimary,

    surfaceVariant = StreamLinkColors.Surface,
    onSurfaceVariant = StreamLinkColors.TextSecondary,

    error = StreamLinkColors.Error,
    onError = Color.White,

    outline = StreamLinkColors.Divider,
    outlineVariant = StreamLinkColors.DividerLight,

    scrim = Color(0x80000000),
    inverseSurface = StreamLinkColors.TextPrimary,
    inverseOnSurface = StreamLinkColors.Background,
    inversePrimary = StreamLinkColors.AccentDark,

    surfaceTint = StreamLinkColors.Accent
)

@Composable
fun StreamLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StreamLinkDarkColorScheme,
        typography = StreamLinkTypography,
        content = content
    )
}
