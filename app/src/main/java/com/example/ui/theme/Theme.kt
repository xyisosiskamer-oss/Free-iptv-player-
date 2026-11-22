package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage

private val TeleWaveDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = OnCyan,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanPrimary,
    secondary = AmberSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = ElevatedSurfaceHigh,
    onSecondaryContainer = SnowText,
    tertiary = CoralAccent,
    onTertiary = OnCoral,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = CoralAccent,
    background = ObsidianBg,
    onBackground = SnowText,
    surface = MidnightSurface,
    onSurface = SnowText,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = ElevatedSurfaceHigh,
    outline = SlateOutline,
    outlineVariant = SlateOutline,
    error = ErrorRed,
    onError = ObsidianBg
)

private val TeleWaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TeleWaveTheme(
    language: AppLanguage = AppLanguage.RUSSIAN,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeleWaveDarkColorScheme,
        typography = teleWaveTypography(language),
        shapes = TeleWaveShapes,
        content = content
    )
}
