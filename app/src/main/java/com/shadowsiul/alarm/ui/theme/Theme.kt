package com.shadowsiul.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFF4C95D)
private val Navy = Color(0xFF1B2430)
private val Mist = Color(0xFFF6F1E9)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Navy,
    background = Navy,
    surface = Color(0xFF243044),
    onBackground = Mist,
    onSurface = Mist,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A6A12),
    onPrimary = Color.White,
    background = Mist,
    surface = Color.White,
    onBackground = Navy,
    onSurface = Navy,
)

@Composable
fun AlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
