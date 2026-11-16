package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContainerLight,
    secondary = TileBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8ECFC),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF281800),
    background = AppBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CardSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFEAF0F2),
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = LossRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0D6),
    onPrimary = Color(0xFF00373A),
    primaryContainer = TealPrimaryDark,
    onPrimaryContainer = Color(0xFFB4F1F4),
    secondary = Color(0xFF6BB8FF),
    onSecondary = Color(0xFF003253),
    secondaryContainer = Color(0xFF004977),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF3E2600),
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF233035),
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF560005)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AmarDokanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
