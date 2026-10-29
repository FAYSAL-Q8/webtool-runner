/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("White / Light"),
    SYSTEM("System")
}

enum class AccentPalette(
    val title: String,
    val color: Color
) {
    INDIGO("Indigo", MidnightIndigo),
    VIOLET("Violet", ElectricViolet),
    CYAN("Cyan", NeonCyan),
    EMERALD("Emerald", VibrantEmerald),
    ROSE("Rose", CoralRose),
    AMBER("Amber", AmberGlow),
    BLUE("Blue", Color(0xFF2563EB)),
    CRIMSON("Crimson", Color(0xFFE11D48))
}

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.DARK,
    val accent: AccentPalette = AccentPalette.INDIGO
)

object AppThemeManager {
    private var prefs: SharedPreferences? = null
    private val _themeConfig = MutableStateFlow(ThemeConfig())
    val themeConfig: StateFlow<ThemeConfig> = _themeConfig.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences("webtool_theme_prefs", Context.MODE_PRIVATE)
        prefs = sp
        val modeStr = sp.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val accentStr = sp.getString("theme_accent", AccentPalette.INDIGO.name) ?: AccentPalette.INDIGO.name

        val mode = try { ThemeMode.valueOf(modeStr) } catch (_: Exception) { ThemeMode.DARK }
        val accent = try { AccentPalette.valueOf(accentStr) } catch (_: Exception) { AccentPalette.INDIGO }

        _themeConfig.value = ThemeConfig(mode, accent)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeConfig.value = _themeConfig.value.copy(mode = mode)
        prefs?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    fun setAccent(accent: AccentPalette) {
        _themeConfig.value = _themeConfig.value.copy(accent = accent)
        prefs?.edit()?.putString("theme_accent", accent.name)?.apply()
    }
}
