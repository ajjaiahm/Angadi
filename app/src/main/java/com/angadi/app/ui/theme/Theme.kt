package com.angadi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = AngadiForest,
    onPrimary = AngadiCard,
    primaryContainer = AngadiMint,
    onPrimaryContainer = AngadiForest,
    secondary = AngadiAmber,
    onSecondary = AngadiInk,
    tertiary = AngadiCoral,
    background = AngadiPaper,
    onBackground = AngadiInk,
    surface = AngadiCard,
    onSurface = AngadiInk,
    surfaceVariant = AngadiMint,
    onSurfaceVariant = AngadiMuted,
    outline = AngadiLine,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD2B1),
    onPrimary = AngadiDark,
    primaryContainer = Color(0xFF294B3A),
    onPrimaryContainer = Color(0xFFD7F3DF),
    secondary = Color(0xFFE8C875),
    onSecondary = AngadiDark,
    tertiary = Color(0xFFFFB5A4),
    background = AngadiDark,
    onBackground = Color(0xFFE6EEE8),
    surface = AngadiDarkSurface,
    onSurface = Color(0xFFE6EEE8),
    surfaceVariant = Color(0xFF27382F),
    onSurfaceVariant = Color(0xFFBAC7BD),
    outline = Color(0xFF405148),
)

@Composable
fun ANGADITheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
