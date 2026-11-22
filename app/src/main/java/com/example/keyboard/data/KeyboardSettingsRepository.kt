package com.example.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import com.example.keyboard.model.AccentColorType
import com.example.keyboard.model.KeyShapeStyle
import com.example.keyboard.model.KeySizeMode
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardSettings
import com.example.keyboard.model.KeyboardThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardSettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("custom_keyboard_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<KeyboardSettings> = _settings.asStateFlow()

    private fun loadSettings(): KeyboardSettings {
        val haptic = prefs.getBoolean(KEY_HAPTIC, true)
        val sound = prefs.getBoolean(KEY_SOUND, false)
        val suggestions = prefs.getBoolean(KEY_SUGGESTIONS, true)
        val autoCorrect = prefs.getBoolean(KEY_AUTOCORRECT, true)
        val autoLang = prefs.getBoolean(KEY_AUTO_LANG, true)
        val showEmoji = prefs.getBoolean(KEY_SHOW_EMOJI, true)
        val showNumberRow = prefs.getBoolean(KEY_SHOW_NUMBER_ROW, false)
        val ledEffect = prefs.getBoolean(KEY_LED_EFFECT, false)
        val vibrationDuration = prefs.getLong(KEY_VIBRATION_DURATION, 20L)

        val langCode = prefs.getString(KEY_LANGUAGE, KeyboardLanguage.PT_BR.code)
        val language = KeyboardLanguage.entries.find { it.code == langCode } ?: KeyboardLanguage.PT_BR

        val themeId = prefs.getString(KEY_THEME, KeyboardThemeType.DARK.id)
        val themeType = KeyboardThemeType.entries.find { it.id == themeId } ?: KeyboardThemeType.DARK

        val accentId = prefs.getString(KEY_ACCENT, AccentColorType.INDIGO.id)
        val accentType = AccentColorType.entries.find { it.id == accentId } ?: AccentColorType.INDIGO

        val adaptiveTheme = prefs.getBoolean(KEY_ADAPTIVE_THEME, true)
        val keySeparation = prefs.getBoolean(KEY_KEY_SEPARATION, true)
        val keySizeId = prefs.getString(KEY_KEY_SIZE_MODE, KeySizeMode.MEDIUM.id)
        val keySizeMode = KeySizeMode.entries.find { it.id == keySizeId } ?: KeySizeMode.MEDIUM
        val keyShapeId = prefs.getString(KEY_KEY_SHAPE_STYLE, KeyShapeStyle.ROUNDED.id)
        val keyShapeStyle = KeyShapeStyle.entries.find { it.id == keyShapeId } ?: KeyShapeStyle.ROUNDED
        val spaceAutoCorrect = prefs.getBoolean(KEY_SPACE_AUTOCORRECT, true)
        val conversationTranslate = prefs.getBoolean(KEY_CONVERSATION_TRANSLATE, false)

        return KeyboardSettings(
            hapticEnabled = haptic,
            vibrationDurationMs = vibrationDuration,
            soundEnabled = sound,
            suggestionsEnabled = suggestions,
            autoCorrectEnabled = autoCorrect,
            spaceAutoCorrectEnabled = spaceAutoCorrect,
            autoLanguageDetectionEnabled = autoLang,
            showEmojiKey = showEmoji,
            showNumberRow = showNumberRow,
            adaptiveThemeEnabled = adaptiveTheme,
            keySeparationEnabled = keySeparation,
            ledEffectEnabled = ledEffect,
            keyShapeStyle = keyShapeStyle,
            keySizeMode = keySizeMode,
            conversationTranslateEnabled = conversationTranslate,
            language = language,
            themeType = themeType,
            accentColorType = accentType
        )
    }

    fun setSpaceAutoCorrectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPACE_AUTOCORRECT, enabled).apply()
        _settings.value = _settings.value.copy(spaceAutoCorrectEnabled = enabled)
    }

    fun setKeyShapeStyle(style: KeyShapeStyle) {
        prefs.edit().putString(KEY_KEY_SHAPE_STYLE, style.id).apply()
        _settings.value = _settings.value.copy(
            keyShapeStyle = style,
            keySeparationEnabled = style.hasElevation
        )
    }

    fun setLedEffectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LED_EFFECT, enabled).apply()
        _settings.value = _settings.value.copy(ledEffectEnabled = enabled)
    }

    fun setAdaptiveThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADAPTIVE_THEME, enabled).apply()
        _settings.value = _settings.value.copy(adaptiveThemeEnabled = enabled)
    }

    fun setKeySeparationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEY_SEPARATION, enabled).apply()
        _settings.value = _settings.value.copy(keySeparationEnabled = enabled)
    }

    fun setKeySizeMode(mode: KeySizeMode) {
        prefs.edit().putString(KEY_KEY_SIZE_MODE, mode.id).apply()
        _settings.value = _settings.value.copy(keySizeMode = mode)
    }

    fun setConversationTranslateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONVERSATION_TRANSLATE, enabled).apply()
        _settings.value = _settings.value.copy(conversationTranslateEnabled = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticEnabled = enabled)
    }

    fun setVibrationDuration(durationMs: Long) {
        prefs.edit().putLong(KEY_VIBRATION_DURATION, durationMs).apply()
        _settings.value = _settings.value.copy(vibrationDurationMs = durationMs)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun setSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUGGESTIONS, enabled).apply()
        _settings.value = _settings.value.copy(suggestionsEnabled = enabled)
    }

    fun setAutoCorrectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOCORRECT, enabled).apply()
        _settings.value = _settings.value.copy(autoCorrectEnabled = enabled)
    }

    fun setAutoLanguageDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LANG, enabled).apply()
        _settings.value = _settings.value.copy(autoLanguageDetectionEnabled = enabled)
    }

    fun setShowEmojiKey(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EMOJI, enabled).apply()
        _settings.value = _settings.value.copy(showEmojiKey = enabled)
    }

    fun setShowNumberRow(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NUMBER_ROW, enabled).apply()
        _settings.value = _settings.value.copy(showNumberRow = enabled)
    }

    fun setLanguage(language: KeyboardLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun setThemeType(themeType: KeyboardThemeType) {
        val editor = prefs.edit().putString(KEY_THEME, themeType.id)
        var updated = _settings.value.copy(themeType = themeType)
        if (themeType == KeyboardThemeType.HONEYCOMB) {
            editor.putString(KEY_KEY_SHAPE_STYLE, KeyShapeStyle.HEXAGON.id)
                .putBoolean(KEY_ADAPTIVE_THEME, false)
                .putString(KEY_ACCENT, AccentColorType.AMBER.id)
            updated = updated.copy(
                keyShapeStyle = KeyShapeStyle.HEXAGON,
                keySeparationEnabled = true,
                adaptiveThemeEnabled = false,
                accentColorType = AccentColorType.AMBER
            )
        }
        editor.apply()
        _settings.value = updated
    }

    fun setAccentColorType(accentColorType: AccentColorType) {
        prefs.edit().putString(KEY_ACCENT, accentColorType.id).apply()
        _settings.value = _settings.value.copy(accentColorType = accentColorType)
    }

    companion object {
        private const val KEY_HAPTIC = "pref_haptic"
        private const val KEY_VIBRATION_DURATION = "pref_vibration_duration"
        private const val KEY_SOUND = "pref_sound"
        private const val KEY_SUGGESTIONS = "pref_suggestions"
        private const val KEY_AUTOCORRECT = "pref_autocorrect"
        private const val KEY_SPACE_AUTOCORRECT = "pref_space_autocorrect"
        private const val KEY_AUTO_LANG = "pref_auto_language"
        private const val KEY_SHOW_EMOJI = "pref_show_emoji"
        private const val KEY_SHOW_NUMBER_ROW = "pref_show_number_row"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_THEME = "pref_theme"
        private const val KEY_ACCENT = "pref_accent"
        private const val KEY_ADAPTIVE_THEME = "pref_adaptive_theme"
        private const val KEY_KEY_SEPARATION = "pref_key_separation"
        private const val KEY_KEY_SHAPE_STYLE = "pref_key_shape_style"
        private const val KEY_LED_EFFECT = "pref_led_effect"
        private const val KEY_KEY_SIZE_MODE = "pref_key_size_mode"
        private const val KEY_CONVERSATION_TRANSLATE = "pref_conversation_translate"

        @Volatile
        private var instance: KeyboardSettingsRepository? = null

        fun getInstance(context: Context): KeyboardSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: KeyboardSettingsRepository(context).also { instance = it }
            }
        }
    }
}
