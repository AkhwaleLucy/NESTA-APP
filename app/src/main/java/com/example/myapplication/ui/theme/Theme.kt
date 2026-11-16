package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = WarmOffWhite,
    primaryContainer = WarmIvory,
    onPrimaryContainer = DarkCharcoal,
    secondary = MutedTerracotta,
    onSecondary = WarmOffWhite,
    secondaryContainer = WarmIvory,
    tertiary = SoftSage,
    onTertiary = DarkCharcoal,
    background = WarmOffWhite,
    onBackground = DarkCharcoal,
    surface = CardBackground,
    onSurface = DarkCharcoal,
    outline = CardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmIvory,
    onPrimary = DeepNavy,
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkTextPrimary,
    secondary = MutedTerracotta,
    onSecondary = DarkBackground,
    tertiary = SoftSage,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
