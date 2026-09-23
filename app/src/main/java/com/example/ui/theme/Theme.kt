package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VidGrabIndigo,
    onPrimary = Color.White,
    primaryContainer = VidGrabPurple.copy(alpha = 0.3f),
    onPrimaryContainer = Color.White,
    secondary = VidGrabPink,
    onSecondary = Color.White,
    tertiary = VidGrabCyan,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = VidGrabIndigo,
    onPrimary = Color.White,
    primaryContainer = VidGrabIndigo.copy(alpha = 0.15f),
    onPrimaryContainer = VidGrabIndigo,
    secondary = VidGrabPurple,
    onSecondary = Color.White,
    tertiary = VidGrabCyan,
    onTertiary = Color.Black,
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    outline = LightBorder
)

@Composable
fun VidGrabTheme(
    darkTheme: Boolean = true, // Default to sleek dark video downloader UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    VidGrabTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
