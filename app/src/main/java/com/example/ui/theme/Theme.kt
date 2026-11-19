package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = WhitePure,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestDark,
    secondary = EmeraldBright,
    onSecondary = WhitePure,
    secondaryContainer = MintSurface,
    onSecondaryContainer = ForestDark,
    tertiary = EmeraldSoft,
    onTertiary = ForestDark,
    background = OffWhiteBackground,
    onBackground = InkDark,
    surface = SurfaceCard,
    onSurface = InkDark,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    outline = OutlineGreen,
    error = ErrorRed,
    onError = WhitePure,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorRed
)

val CleanCityShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CleanCityTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CleanCityTypography,
        shapes = CleanCityShapes,
        content = content
    )
}
