package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldSecondary,
    onSecondary = OnGoldSecondary,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = ParchmentBackground,
    onBackground = OnParchment,
    surface = ParchmentSurface,
    onSurface = OnParchment,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = OnParchmentVariant,
    outline = ParchmentOutline,
    error = ErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NightEmeraldPrimary,
    onPrimary = OnNightEmeraldPrimary,
    primaryContainer = NightEmeraldContainer,
    onPrimaryContainer = OnNightEmeraldContainer,
    secondary = NightGoldSecondary,
    onSecondary = OnNightGoldSecondary,
    secondaryContainer = NightGoldContainer,
    onSecondaryContainer = OnNightGoldContainer,
    tertiary = NightTealTertiary,
    onTertiary = OnNightTealTertiary,
    tertiaryContainer = NightTealContainer,
    onTertiaryContainer = OnNightTealContainer,
    background = NightBackground,
    onBackground = OnNightBackground,
    surface = NightSurface,
    onSurface = OnNightBackground,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = OnNightVariant,
    outline = NightOutline,
    error = ErrorDark
)

val QuranShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AlQuranTheme(
    darkModeSetting: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (darkModeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuranTypography,
            shapes = QuranShapes,
            content = content
        )
    }
}
