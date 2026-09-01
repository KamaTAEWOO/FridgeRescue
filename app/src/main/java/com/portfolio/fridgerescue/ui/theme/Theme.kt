package com.portfolio.fridgerescue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FridgeRescueColors = lightColorScheme(
    primary = Color(0xFF17221D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAFF62),
    onPrimaryContainer = Color(0xFF17221D),
    secondary = Color(0xFF5775FF),
    background = Color(0xFFF8F7F1),
    onBackground = Color(0xFF17221D),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF17221D),
)

@Composable
fun FridgeRescueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FridgeRescueColors,
        content = content,
    )
}
