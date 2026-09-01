package com.portfolio.fridgerescue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
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

private val FridgeRescueDarkColors = darkColorScheme(
    primary = Color(0xFFCAFF62),
    onPrimary = Color(0xFF1D2A20),
    primaryContainer = Color(0xFF40541C),
    onPrimaryContainer = Color(0xFFE1FFAA),
    secondary = Color(0xFFB6C4FF),
    secondaryContainer = Color(0xFF334A9B),
    background = Color(0xFF111512),
    onBackground = Color(0xFFE3E7E1),
    surface = Color(0xFF191D1A),
    onSurface = Color(0xFFE3E7E1),
    surfaceVariant = Color(0xFF343A34),
    onSurfaceVariant = Color(0xFFC4CBC2),
)

@Composable
fun FridgeRescueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) FridgeRescueDarkColors else FridgeRescueColors,
        content = content,
    )
}
