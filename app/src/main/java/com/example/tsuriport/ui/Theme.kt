package com.example.tsuriport.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A9E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF4F616E),
    background = Color(0xFFF6FAFE),
    surface = Color(0xFFF6FAFE),
    surfaceVariant = Color(0xFFDDE3EA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CCDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCAE6FF),
    secondary = Color(0xFFB7C9D9),
    background = Color(0xFF0F1418),
    surface = Color(0xFF0F1418),
    surfaceVariant = Color(0xFF41484D),
)

@Composable
fun TsuriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
