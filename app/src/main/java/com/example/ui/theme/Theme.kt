/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    val themeConfig by AppThemeManager.themeConfig.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (themeConfig.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val accent = themeConfig.accent.color

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = Slate800,
            onSecondaryContainer = Color.White,
            tertiary = NeonCyan,
            onTertiary = Slate950,
            background = Slate950,
            onBackground = Color(0xFFF8FAFC),
            surface = Slate900,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Slate800,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Slate700,
            outlineVariant = Slate800
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.15f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE2E8F0),
            onSecondaryContainer = Color(0xFF0F172A),
            tertiary = NeonCyan,
            onTertiary = Slate950,
            background = Color(0xFFF1F5F9),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF1E293B),
            outline = Color(0xFF94A3B8),
            outlineVariant = Color(0xFFCBD5E1)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
