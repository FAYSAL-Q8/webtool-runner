/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object GlassDefaults {
    val RadiusLarge = 20.dp
    val RadiusMedium = 14.dp
    val RadiusSmall = 10.dp
}

@Composable
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(GlassDefaults.RadiusMedium),
    borderWidth: Dp = 1.dp,
    customAlpha: Float? = null
): Modifier {
    val themeConfig by AppThemeManager.themeConfig.collectAsStateWithLifecycle()
    val isDark = when (themeConfig.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (isDark) {
        DarkGlassBackground.copy(alpha = customAlpha ?: 0.78f)
    } else {
        Color.White.copy(alpha = customAlpha ?: 0.94f)
    }

    val borderColor = if (isDark) {
        Color(0x33FFFFFF)
    } else {
        Color(0x260F172A)
    }

    return this
        .clip(shape)
        .background(bgColor)
        .border(BorderStroke(borderWidth, borderColor), shape)
}

@Composable
fun Modifier.glassmorphismGradient(
    shape: Shape = RoundedCornerShape(GlassDefaults.RadiusMedium),
    borderWidth: Dp = 1.dp
): Modifier {
    val themeConfig by AppThemeManager.themeConfig.collectAsStateWithLifecycle()
    val isDark = when (themeConfig.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val brush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x38FFFFFF),
                Color(0x101E293B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x200F172A),
                Color(0x080F172A)
            )
        )
    }

    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x260F172A)

    return this
        .clip(shape)
        .background(brush)
        .border(BorderStroke(borderWidth, borderColor), shape)
}
