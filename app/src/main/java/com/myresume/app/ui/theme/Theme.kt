package com.myresume.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4F72),
    onPrimary = Color.White,
    secondary = Color(0xFF2874A6),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    onBackground = Color(0xFF1C2833),
    onSurface = Color(0xFF1C2833)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DADE2),
    onPrimary = Color(0xFF0B1C2C),
    secondary = Color(0xFF85C1E9),
    background = Color(0xFF0F1720),
    surface = Color(0xFF1B2631),
    onBackground = Color(0xFFF4F6F7),
    onSurface = Color(0xFFF4F6F7)
)

@Composable
fun MyResumeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
