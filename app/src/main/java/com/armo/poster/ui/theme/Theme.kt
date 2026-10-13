package com.armo.poster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VoltAccent,
    onPrimary = BgDark,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = VoltAccent,
    secondary = VoltAccent,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = PanelBackground,
    onSurfaceVariant = TextBody,
    outline = HairlineColor,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun ARMOPosterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
