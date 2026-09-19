package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeminiWhiteColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE),
    onPrimaryContainer = GoogleBlue,
    secondary = GoogleBlueDark,
    onSecondary = Color.White,
    background = GoogleSurfaceLight,
    onBackground = GoogleTextDark,
    surface = GoogleSurfaceLight,
    onSurface = GoogleTextDark,
    surfaceVariant = GoogleSurfaceVariantLight,
    onSurfaceVariant = GoogleTextSecondary,
    outline = GoogleOutlineLight,
    outlineVariant = Color(0xFFF1F3F4)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GeminiWhiteColorScheme,
        typography = Typography,
        content = content
    )
}

