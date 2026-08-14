package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GlassCyan,
    onPrimary = GlassObsidian,
    primaryContainer = GlassViolet,
    onPrimaryContainer = TextPrimary,
    secondary = GlassPurple,
    onSecondary = Color.White,
    tertiary = GlassPink,
    background = GlassObsidian,
    onBackground = TextPrimary,
    surface = GlassObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderWhite,
    outlineVariant = Color(0x1FFFFFFF)
)

private val LightColorScheme = darkColorScheme(
    // We emphasize the glassmorphism dark aesthetic by default for optimal shader vibrancy
    primary = GlassCyanDim,
    onPrimary = Color.White,
    primaryContainer = GlassViolet,
    onPrimaryContainer = TextPrimary,
    secondary = GlassPurple,
    onSecondary = Color.White,
    tertiary = GlassPink,
    background = GlassObsidian,
    onBackground = TextPrimary,
    surface = GlassObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderWhite,
    outlineVariant = Color(0x1FFFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
