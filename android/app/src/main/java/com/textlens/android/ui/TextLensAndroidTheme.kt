package com.textlens.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TextLensColors = darkColorScheme(
    primary = Color(0xFFF5C84A),
    onPrimary = Color(0xFF070707),
    background = Color(0xFF070707),
    onBackground = Color(0xFFF7F0DC),
    surface = Color(0xFF11100D),
    onSurface = Color(0xFFF7F0DC),
)

@Composable
fun TextLensAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TextLensColors,
        content = content,
    )
}
