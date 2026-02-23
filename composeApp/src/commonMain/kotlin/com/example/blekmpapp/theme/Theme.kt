package com.example.blekmpapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    background = Background,
    onBackground = TextPrimary,
    surface = DarkBlue,
    onSurface = TextPrimary,
    secondary = LightBlue,
    onSecondary = DarkBlue,
    error = Red,
    surfaceVariant = Color(0xFF334155)
)

@Composable
fun BLEAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}