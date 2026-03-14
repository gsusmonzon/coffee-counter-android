package com.gsusmonzon.coffeecounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Espresso,
    onPrimary = Foam,
    primaryContainer = Latte,
    onPrimaryContainer = WarmInk,
    secondary = Caramel,
    onSecondary = Foam,
    secondaryContainer = CreamShadow,
    onSecondaryContainer = WarmInk,
    tertiary = Cocoa,
    onTertiary = Foam,
    tertiaryContainer = Latte,
    onTertiaryContainer = WarmInk,
    background = Oat,
    onBackground = WarmInk,
    surface = Foam,
    onSurface = WarmInk,
    surfaceVariant = Latte,
    onSurfaceVariant = WarmInkSoft,
    outline = CreamShadow,
    error = WarningWarm,
    onError = Foam,
    errorContainer = WarningContainerWarm,
    onErrorContainer = WarningOnContainerWarm,
)

private val DarkColorScheme = darkColorScheme(
    primary = Latte,
    onPrimary = EspressoDark,
    primaryContainer = Espresso,
    onPrimaryContainer = Foam,
    secondary = Caramel,
    onSecondary = EspressoDark,
    secondaryContainer = Cocoa,
    onSecondaryContainer = Foam,
    tertiary = CreamShadow,
    onTertiary = EspressoDark,
    tertiaryContainer = Mocha,
    onTertiaryContainer = Foam,
    background = EspressoDark,
    onBackground = Oat,
    surface = Color(0xFF33231A),
    onSurface = Oat,
    surfaceVariant = Color(0xFF493427),
    onSurfaceVariant = Latte,
    outline = Mocha,
    error = Color(0xFFFFB59F),
    onError = Color(0xFF601F0B),
    errorContainer = Color(0xFF7D2F17),
    onErrorContainer = Color(0xFFFFDBD1),
)

@Composable
fun CoffeeCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
