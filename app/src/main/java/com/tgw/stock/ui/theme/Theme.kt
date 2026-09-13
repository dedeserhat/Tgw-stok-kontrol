package com.tgw.stock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = TgwPrimary,
    onPrimary = TgwOnPrimary,
    secondary = TgwSecondary,
    background = TgwBackgroundLight,
    surface = TgwSurfaceLight,
    surfaceVariant = Color(0xFFE7EBE8)
)

private val DarkColors = darkColorScheme(
    primary = TgwPrimaryDark,
    onPrimary = Color(0xFF00391F),
    secondary = Color(0xFFB2CCBE),
    background = TgwBackgroundDark,
    surface = TgwSurfaceDark
)

@Composable
fun TgwStockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TgwTypography,
        content = content
    )
}
