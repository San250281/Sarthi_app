package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cos_Primary,
    secondary = Cos_Secondary,
    background = Cos_Background,
    surface = Cos_Surface,
    surfaceVariant = Cos_SurfaceVariant,
    onBackground = Cos_OnBackground,
    onSurface = Cos_OnSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    outline = Color(0xFF2D314A)
)

private val LightColorScheme = lightColorScheme(
    primary = Day_Primary,
    secondary = Day_Secondary,
    background = Day_Background,
    surface = Day_Surface,
    surfaceVariant = Day_SurfaceVariant,
    onBackground = Day_OnBackground,
    onSurface = Day_OnSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = Color(0xFFD5C8B4)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
