package com.wgandroid.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WireguardBlue80,
    onPrimary = WireguardBlue20,
    primaryContainer = WireguardBlue30,
    onPrimaryContainer = WireguardBlue90,
    secondary = WireguardGreen80,
    onSecondary = WireguardGreen20,
    secondaryContainer = WireguardGreen30,
    onSecondaryContainer = WireguardGreen90,
    tertiary = WireguardOrange80,
    onTertiary = WireguardOrange20,
    tertiaryContainer = WireguardOrange30,
    onTertiaryContainer = WireguardOrange90
)

private val LightColorScheme = lightColorScheme(
    primary = WireguardBlue40,
    onPrimary = WireguardBlue100,
    primaryContainer = WireguardBlue90,
    onPrimaryContainer = WireguardBlue10,
    secondary = WireguardGreen40,
    onSecondary = WireguardGreen100,
    secondaryContainer = WireguardGreen90,
    onSecondaryContainer = WireguardGreen10,
    tertiary = WireguardOrange40,
    onTertiary = WireguardOrange100,
    tertiaryContainer = WireguardOrange90,
    onTertiaryContainer = WireguardOrange10
)

@Composable
fun WgAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 