package com.pinky.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = Color(0xFF381528),
    primaryContainer = PinkContainer,
    onPrimaryContainer = OnPinkContainer,
    secondary = PinkAccent,
    onSecondary = Color.Black,
    secondaryContainer = CharcoalSurfaceVariant,
    onSecondaryContainer = PinkPrimaryLight,
    tertiary = PinkPrimaryDark,
    onTertiary = Color.White,
    background = CharcoalBackground,
    onBackground = TextWhite,
    surface = CharcoalSurface,
    onSurface = TextWhite,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = CharcoalBorder,
    outlineVariant = CharcoalBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF831843),
    secondary = PinkAccent,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Color(0xFF1F2937),
    tertiary = PinkPrimary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextDark,
    surface = LightSurface,
    onSurface = LightTextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    outlineVariant = Color(0xFFD1D5DB)
)

@Composable
fun PinkyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PinkyTypography,
        content = content
    )
}
