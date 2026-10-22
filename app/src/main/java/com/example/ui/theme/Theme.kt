package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color(0xFF00391A),
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF00391A),
    background = PageBackgroundDark,
    surface = CardBackgroundDark,
    surfaceVariant = Color(0xFF223328),
    onBackground = TextLight,
    onSurface = TextLight,
    outline = CardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = LightMintContainer,
    onPrimaryContainer = OnLightMintContainer,
    secondary = ForestGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEE9),
    onSecondaryContainer = Color(0xFF203A27),
    background = PageBackgroundLight,
    surface = CardBackgroundLight,
    surfaceVariant = Color(0xFFF5F8F5),
    onBackground = TextDark,
    onSurface = TextDark,
    outline = Color(0xFFCBD5CE)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep consistent brand colors for Data Entry Register
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
