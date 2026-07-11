package com.marghazari.coveredcall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B8E5A)
private val GreenDark = Color(0xFF0F5C38)
private val Red = Color(0xFFD32F2F)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenDark,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = Green,
    secondary = GreenDark,
    error = Red
)

@Composable
fun CoveredCallTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
