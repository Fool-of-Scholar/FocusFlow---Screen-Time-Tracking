package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = GalacticTeal,
    onPrimary = DeepMidnight,
    primaryContainer = SlateCard,
    onPrimaryContainer = TextPrimary,
    secondary = CyberGreen,
    onSecondary = Color.Black,
    secondaryContainer = SlateCard,
    onSecondaryContainer = TextSecondary,
    tertiary = NeonAmber,
    error = AlertCrimson,
    background = DeepMidnight,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF242730),
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    outlineVariant = Color(0xFF2D323E)
)

@Composable
fun FocusFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
