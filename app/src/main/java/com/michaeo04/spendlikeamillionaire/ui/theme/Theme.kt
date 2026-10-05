package com.michaeo04.spendlikeamillionaire.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B6B4A)
private val Gold = Color(0xFFF5C542)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEBD3),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF8A6D00),
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    background = Color(0xFFF6F8F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3EAE2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8A8),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF0B5233),
    onPrimaryContainer = Color(0xFFBDEBD3),
    secondary = Gold,
    secondaryContainer = Color(0xFF5B4600),
    onSecondaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0E1512),
    surface = Color(0xFF151D19),
    surfaceVariant = Color(0xFF26322C),
)

@Composable
fun SpendTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
