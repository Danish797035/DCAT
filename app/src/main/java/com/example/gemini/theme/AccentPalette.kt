package com.example.gemini.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * D~C@T: user-selectable accent colour. [ClaudeTerracotta] / [ClaudeTerracottaDark] read the current accent, so the
 * whole app (and MaterialTheme.colorScheme.primary) follows the choice without touching every screen.
 */
data class AccentPreset(
    val id: String,
    val label: String,
    val light: Color,
    val lightDeep: Color,
    val dark: Color,
    val darkDeep: Color,
    /** Text/icon colour drawn on top of the accent. */
    val onLight: Color = Color.White,
    val onDark: Color = Color.White
)

object AccentPalette {
    private const val PREFS = "dcat_theme"
    private const val KEY_ACCENT = "accent_preset"

    val presets: List<AccentPreset> = listOf(
        AccentPreset("AMBER", "Amber", Color(0xFFD97706), Color(0xFFB45309), Color(0xFFD97706), Color(0xFFB45309)),
        AccentPreset(
            "KITTEN", "Kitten", Color(0xFF111111), Color(0xFF000000), Color(0xFFF2F2F2), Color(0xFFD4D4D4),
            onLight = Color.White, onDark = Color(0xFF111111)
        ),
        AccentPreset("BLUE", "Ocean", Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF2563EB)),
        AccentPreset("PURPLE", "Violet", Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFF7C3AED)),
        AccentPreset("GREEN", "Emerald", Color(0xFF059669), Color(0xFF047857), Color(0xFF10B981), Color(0xFF059669)),
        AccentPreset("ROSE", "Rose", Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFFF43F5E), Color(0xFFE11D48)),
        AccentPreset("TEAL", "Teal", Color(0xFF0D9488), Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFF0D9488))
    )

    var presetId by mutableStateOf("AMBER")
        private set

    /** Set by GeminiTheme so accents with separate light/dark shades (Kitten) pick the right one. */
    var isDark by mutableStateOf(false)
        internal set

    private var loaded = false

    val preset: AccentPreset get() = presets.firstOrNull { it.id == presetId } ?: presets.first()
    val accent: Color get() = preset.let { if (isDark) it.dark else it.light }
    val accentDeep: Color get() = preset.let { if (isDark) it.darkDeep else it.lightDeep }
    val onAccent: Color get() = preset.let { if (isDark) it.onDark else it.onLight }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        presetId = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACCENT, "AMBER") ?: "AMBER"
    }

    fun select(context: Context, id: String) {
        presetId = id
        loaded = true
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACCENT, id).apply()
    }
}
