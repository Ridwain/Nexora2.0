package com.nexora.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = FieldGreen,
    onPrimary = Paper,
    secondary = Copper,
    onSecondary = Paper,
    background = Fog,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Line,
    onSurfaceVariant = ColorTokens.Muted
)

private val DarkColors = darkColorScheme(
    primary = ColorTokens.Sage,
    onPrimary = Ink,
    secondary = ColorTokens.Clay,
    onSecondary = Ink,
    background = ColorTokens.Night,
    onBackground = Paper,
    surface = ColorTokens.Panel,
    onSurface = Paper,
    surfaceVariant = ColorTokens.PanelLine,
    onSurfaceVariant = ColorTokens.Soft
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexoraTypography,
        content = content
    )
}

private object ColorTokens {
    val Muted = androidx.compose.ui.graphics.Color(0xFF5D675F)
    val Sage = androidx.compose.ui.graphics.Color(0xFFB5D0BA)
    val Clay = androidx.compose.ui.graphics.Color(0xFFE3AF83)
    val Night = androidx.compose.ui.graphics.Color(0xFF111713)
    val Panel = androidx.compose.ui.graphics.Color(0xFF1A221D)
    val PanelLine = androidx.compose.ui.graphics.Color(0xFF303A33)
    val Soft = androidx.compose.ui.graphics.Color(0xFFC9D1C8)
}

