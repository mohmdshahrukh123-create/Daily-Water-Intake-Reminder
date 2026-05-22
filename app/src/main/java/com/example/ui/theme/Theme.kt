package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    secondary = DarkBlueSecondary,
    background = DarkBlueBackground,
    surface = DarkBlueBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightBluePrimary,
    secondary = LightBlueSecondary,
    background = LightBlueBackground,
    surface = CrispWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1B2A4A),
    onSurface = Color(0xFF1B2A4A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // We enforce our premium hand-styled "Blue & White" theme to prevent generic Android dynamic coloring from washing out the visual experience.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
