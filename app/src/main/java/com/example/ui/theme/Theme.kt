package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryPurple,
    onPrimary = DarkOnPrimaryPurple,
    primaryContainer = DarkPrimaryContainerPurple,
    onPrimaryContainer = DarkOnPrimaryContainerPurple,
    secondary = DarkSecondaryPurple,
    onSecondary = DarkOnSecondaryPurple,
    secondaryContainer = DarkSecondaryContainerPurple,
    onSecondaryContainer = DarkOnSecondaryContainerPurple,
    tertiary = DarkTertiaryDustyRose,
    onTertiary = DarkOnTertiaryDustyRose,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnDarkBackground,
    onSurface = OnDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerPurple,
    onPrimaryContainer = OnPrimaryContainerPurple,
    secondary = SecondaryPurple,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerPurple,
    onSecondaryContainer = OnSecondaryContainerPurple,
    tertiary = TertiaryDustyRose,
    onTertiary = OnTertiaryWhite,
    tertiaryContainer = TertiaryContainerDustyRose,
    onTertiaryContainer = OnTertiaryContainerDustyRose,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = OnLightBackground,
    onSurface = OnLightSurface
)

@Composable
fun HcmSmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
