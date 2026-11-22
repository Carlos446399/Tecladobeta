package com.example.keyboard.model

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeType(val id: String, val displayName: String) {
    DARK("dark", "Escuro Moderno"),
    LIGHT("light", "Claro Elegante"),
    AMOLED("amoled", "Preto Puro (AMOLED)"),
    PASTEL("pastel", "Pastel Suave"),
    NEON("neon", "Cyber Neon"),
    AURORA("aurora", "Aurora Boreal"),
    HOLOGRAPHIC("holographic", "Holográfico"),
    HONEYCOMB("honeycomb", "Colmeia Âmbar")
}

enum class AccentColorType(val id: String, val displayName: String, val color: Color) {
    INDIGO("indigo", "Índigo", Color(0xFF6366F1)),
    EMERALD("emerald", "Esmeralda", Color(0xFF10B981)),
    VIOLET("violet", "Violeta", Color(0xFF8B5CF6)),
    AMBER("amber", "Âmbar", Color(0xFFF59E0B)),
    ROSE("rose", "Rosa", Color(0xFFF43F5E)),
    CYAN("cyan", "Ciano", Color(0xFF06B6D4))
}

enum class KeyboardLanguage(val code: String, val displayName: String, val shortLabel: String) {
    PT_BR("pt_BR", "Português (Brasil)", "PT"),
    EN_US("en_US", "English (US)", "EN")
}

enum class KeySizeMode(val id: String, val displayName: String, val heightDp: Int, val fontScale: Float) {
    SMALL("small", "Compacto", 40, 0.9f),
    MEDIUM("medium", "Padrão", 46, 1.0f),
    LARGE("large", "Amplo / Conforto", 54, 1.15f)
}

enum class KeyShapeStyle(
    val id: String,
    val displayName: String,
    val cornerRadiusDp: Int,
    val hGapDp: Float,
    val vGapDp: Float,
    val hasElevation: Boolean,
    val description: String
) {
    ROUNDED("rounded", "Arredondadas", 8, 2.5f, 3.0f, true, "Bordas curvas confortáveis (Padrão)"),
    CIRCULAR("circular", "Redondas", 22, 3.0f, 3.5f, true, "Teclas em cápsula / circulares"),
    SQUARE("square", "Quadradas", 2, 2.0f, 2.5f, true, "Bordas retas e formato geométrico"),
    BORDERLESS("borderless", "Sem Separação", 0, 0.5f, 1.0f, false, "Design contínuo e plano sem linhas"),
    HEXAGON("hexagon", "Hexagonais", -1, 1.4f, 1.8f, true, "Grade colmeia com borda fina âmbar")
}

enum class ShiftState {
    OFF,
    SHIFTED,
    CAPS_LOCKED
}

enum class KeyboardMode {
    LETTERS,
    NUMBERS,
    SYMBOLS,
    EMOJI,
    CLIPBOARD,
    TRANSLATE
}

data class KeyboardSettings(
    val hapticEnabled: Boolean = true,
    val vibrationDurationMs: Long = 20L,
    val soundEnabled: Boolean = false,
    val suggestionsEnabled: Boolean = true,
    val autoCorrectEnabled: Boolean = true,
    val spaceAutoCorrectEnabled: Boolean = true,
    val autoLanguageDetectionEnabled: Boolean = true,
    val showEmojiKey: Boolean = true,
    val showNumberRow: Boolean = false,
    val adaptiveThemeEnabled: Boolean = true,
    val keySeparationEnabled: Boolean = true,
    val ledEffectEnabled: Boolean = false,
    val keyShapeStyle: KeyShapeStyle = KeyShapeStyle.ROUNDED,
    val keySizeMode: KeySizeMode = KeySizeMode.MEDIUM,
    val conversationTranslateEnabled: Boolean = false,
    val language: KeyboardLanguage = KeyboardLanguage.PT_BR,
    val themeType: KeyboardThemeType = KeyboardThemeType.DARK,
    val accentColorType: AccentColorType = AccentColorType.INDIGO
)

object AppThemeMapper {
    /**
     * Resolves context-aware dynamic theme and accent matching the target app (Modo Camaleão)
     */
    fun resolve(packageName: String?): Pair<KeyboardThemeType, AccentColorType>? {
        if (packageName == null) return null
        val pkg = packageName.lowercase()

        return when {
            // WhatsApp -> Dark + Emerald Accent
            pkg.contains("whatsapp") -> Pair(KeyboardThemeType.DARK, AccentColorType.EMERALD)
            // Instagram -> Holographic + Rose Accent
            pkg.contains("instagram") -> Pair(KeyboardThemeType.HOLOGRAPHIC, AccentColorType.ROSE)
            // Gmail & Office Docs -> Clean Light + Cyan Accent
            pkg.contains("gmail") || pkg.contains("mail") || pkg.contains("docs") -> Pair(KeyboardThemeType.LIGHT, AccentColorType.CYAN)
            // Telegram -> Aurora + Cyan Accent
            pkg.contains("telegram") -> Pair(KeyboardThemeType.AURORA, AccentColorType.CYAN)
            // Discord / Slack -> Cyber Neon + Violet/Indigo
            pkg.contains("discord") || pkg.contains("slack") -> Pair(KeyboardThemeType.NEON, AccentColorType.INDIGO)
            // Twitter / X / YouTube / Netflix -> Pure AMOLED + Rose/Cyan
            pkg.contains("twitter") || pkg.contains("netflix") || pkg.contains("youtube") -> Pair(KeyboardThemeType.AMOLED, AccentColorType.ROSE)
            // Reddit / Browser -> Dark + Amber
            pkg.contains("reddit") || pkg.contains("chrome") || pkg.contains("browser") -> Pair(KeyboardThemeType.DARK, AccentColorType.AMBER)
            else -> null
        }
    }
}

data class KeyboardThemeColors(
    val background: Color,
    val keyBackground: Color,
    val keyBackgroundPressed: Color,
    val actionKeyBackground: Color,
    val accentKeyBackground: Color,
    val keyTextColor: Color,
    val keySecondaryTextColor: Color,
    val accentTextColor: Color,
    val suggestionBackground: Color,
    val suggestionTextColor: Color,
    val dividerColor: Color,
    val keyBorderColor: Color? = null,
    val isIncognitoTint: Boolean = false,
    val ledEffectEnabled: Boolean = false
)

object KeyboardThemeColorsFactory {
    fun getColors(themeType: KeyboardThemeType, accentType: AccentColorType): KeyboardThemeColors {
        val accent = accentType.color
        return when (themeType) {
            KeyboardThemeType.DARK -> KeyboardThemeColors(
                background = Color(0xFF16181F),
                keyBackground = Color(0xFF262936),
                keyBackgroundPressed = Color(0xFF383C4E),
                actionKeyBackground = Color(0xFF1E212D),
                accentKeyBackground = accent,
                keyTextColor = Color(0xFFF1F3F9),
                keySecondaryTextColor = Color(0xFF8E95AA),
                accentTextColor = Color.White,
                suggestionBackground = Color(0xFF1C1E27),
                suggestionTextColor = Color(0xFFECEFF8),
                dividerColor = Color(0xFF2B2E3D)
            )
            KeyboardThemeType.LIGHT -> KeyboardThemeColors(
                background = Color(0xFFF1F4F9),
                keyBackground = Color(0xFFFFFFFF),
                keyBackgroundPressed = Color(0xFFE2E6EE),
                actionKeyBackground = Color(0xFFDFE4ED),
                accentKeyBackground = accent,
                keyTextColor = Color(0xFF1A1C23),
                keySecondaryTextColor = Color(0xFF6B7280),
                accentTextColor = Color.White,
                suggestionBackground = Color(0xFFE9EDF5),
                suggestionTextColor = Color(0xFF1A1C23),
                dividerColor = Color(0xFFD6DBE5)
            )
            KeyboardThemeType.AMOLED -> KeyboardThemeColors(
                background = Color(0xFF000000),
                keyBackground = Color(0xFF141414),
                keyBackgroundPressed = Color(0xFF282828),
                actionKeyBackground = Color(0xFF0D0D0D),
                accentKeyBackground = accent,
                keyTextColor = Color(0xFFFFFFFF),
                keySecondaryTextColor = Color(0xFF737373),
                accentTextColor = Color.White,
                suggestionBackground = Color(0xFF0A0A0A),
                suggestionTextColor = Color(0xFFF5F5F5),
                dividerColor = Color(0xFF202020)
            )
            KeyboardThemeType.PASTEL -> KeyboardThemeColors(
                background = Color(0xFFFAF7F5),
                keyBackground = Color(0xFFFFFFFF),
                keyBackgroundPressed = Color(0xFFEAE5E1),
                actionKeyBackground = Color(0xFFEEE8E4),
                accentKeyBackground = accent,
                keyTextColor = Color(0xFF3C3836),
                keySecondaryTextColor = Color(0xFF8D8580),
                accentTextColor = Color.White,
                suggestionBackground = Color(0xFFF4EEEA),
                suggestionTextColor = Color(0xFF3C3836),
                dividerColor = Color(0xFFE3DDD7)
            )
            KeyboardThemeType.NEON -> KeyboardThemeColors(
                background = Color(0xFF080B11),
                keyBackground = Color(0xFF101726),
                keyBackgroundPressed = Color(0xFF1E293B),
                actionKeyBackground = Color(0xFF0D121F),
                accentKeyBackground = Color(0xFF06B6D4), // Cyber Cyan Neon
                keyTextColor = Color(0xFFF0F9FF),
                keySecondaryTextColor = Color(0xFF38BDF8),
                accentTextColor = Color(0xFF030712),
                suggestionBackground = Color(0xFF090D16),
                suggestionTextColor = Color(0xFFE0F2FE),
                dividerColor = Color(0xFF0284C7).copy(alpha = 0.35f),
                keyBorderColor = Color(0xFF06B6D4).copy(alpha = 0.45f)
            )
            KeyboardThemeType.AURORA -> KeyboardThemeColors(
                background = Color(0xFF0B1021),
                keyBackground = Color(0xFF161F38),
                keyBackgroundPressed = Color(0xFF223056),
                actionKeyBackground = Color(0xFF11182E),
                accentKeyBackground = Color(0xFF10B981), // Aurora Emerald Teal
                keyTextColor = Color(0xFFF8FAFC),
                keySecondaryTextColor = Color(0xFF34D399),
                accentTextColor = Color(0xFF022C22),
                suggestionBackground = Color(0xFF0E152B),
                suggestionTextColor = Color(0xFFECFDF5),
                dividerColor = Color(0xFF10B981).copy(alpha = 0.3f),
                keyBorderColor = Color(0xFF10B981).copy(alpha = 0.35f)
            )
            KeyboardThemeType.HOLOGRAPHIC -> KeyboardThemeColors(
                background = Color(0xFF120E24),
                keyBackground = Color(0xFF221A45),
                keyBackgroundPressed = Color(0xFF342866),
                actionKeyBackground = Color(0xFF1A1436),
                accentKeyBackground = Color(0xFFC084FC), // Holographic Iridescent Violet
                keyTextColor = Color(0xFFFAF5FF),
                keySecondaryTextColor = Color(0xFFA855F7),
                accentTextColor = Color(0xFF1E1035),
                suggestionBackground = Color(0xFF171130),
                suggestionTextColor = Color(0xFFF3E8FF),
                dividerColor = Color(0xFFA855F7).copy(alpha = 0.3f),
                keyBorderColor = Color(0xFFC084FC).copy(alpha = 0.4f)
            )
            KeyboardThemeType.HONEYCOMB -> KeyboardThemeColors(
                background = Color(0xFF090A0D),
                keyBackground = Color(0xFF181612),
                keyBackgroundPressed = Color(0xFF3C2C12),
                actionKeyBackground = Color(0xFF14120E),
                accentKeyBackground = Color(0xFFF2B84B),
                keyTextColor = Color(0xFFFFF8E8),
                keySecondaryTextColor = Color(0xFFC7A96B),
                accentTextColor = Color(0xFF1C1608),
                suggestionBackground = Color(0xFF100F0C),
                suggestionTextColor = Color(0xFFFFF8E8),
                dividerColor = Color(0xFFB9862F).copy(alpha = 0.35f),
                keyBorderColor = Color(0xFFD5A84C).copy(alpha = 0.75f)
            )
        }
    }
}
