package com.example.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.keyboard.data.AppPhrasesRepository
import com.example.keyboard.data.InferredTone
import com.example.keyboard.data.db.ClipboardItemEntity
import com.example.keyboard.model.AppThemeMapper
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardMode
import com.example.keyboard.model.KeyboardSettings
import com.example.keyboard.model.KeyboardThemeColorsFactory
import com.example.keyboard.model.ShiftState
import com.example.stickers.GiphyMediaItem
import com.example.stickers.StickerItem
import com.example.translation.TranslationManager
import com.example.translation.TranslationPanelView

@Composable
fun KeyboardMainView(
    settings: KeyboardSettings,
    imeAction: Int,
    suggestions: List<String>,
    quickPhrases: List<String> = emptyList(),
    quickPhraseCategoryBadge: String = "Geral",
    activePackageName: String? = null,
    inferredTone: InferredTone = InferredTone.NEUTRAL,
    isVoiceRecording: Boolean = false,
    isSecureFieldActive: Boolean = false,
    isIncognitoActive: Boolean = false,
    clipboardClips: List<ClipboardItemEntity> = emptyList(),
    onCharTyped: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onQuickPhraseSelected: (String) -> Unit = {},
    onToggleIncognito: () -> Unit = {},
    onToggleVoiceInput: () -> Unit = {},
    onLanguageSwitched: () -> Unit,
    onDeleteClip: (Long) -> Unit = {},
    onClearAllClips: () -> Unit = {},
    onStickerSelected: (StickerItem) -> Unit = {},
    onGifSelected: (GiphyMediaItem) -> Unit = {},
    onOpenOcr: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onToggleFloatingReader: (Boolean) -> Unit = {},
    onHideKeyboard: () -> Unit,
    getSelectedTextForTranslation: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val translationManager = remember(context) { TranslationManager.getInstance(context) }

    var mode by remember { mutableStateOf(KeyboardMode.LETTERS) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isQuickPhrasesMode by remember { mutableStateOf(false) }
    var translationInputText by remember { mutableStateOf("") }
    var translationKeyboardMode by remember { mutableStateOf(KeyboardMode.LETTERS) }
    var translationShiftState by remember { mutableStateOf(ShiftState.OFF) }

    // Adaptive Chameleon Theme Resolution (overrides default theme if enabled and match exists)
    val effectiveColors = remember(settings.themeType, settings.accentColorType, settings.adaptiveThemeEnabled, settings.ledEffectEnabled, activePackageName) {
        val adaptivePair = if (settings.adaptiveThemeEnabled) AppThemeMapper.resolve(activePackageName) else null
        val targetTheme = adaptivePair?.first ?: settings.themeType
        val targetAccent = adaptivePair?.second ?: settings.accentColorType
        KeyboardThemeColorsFactory.getColors(targetTheme, targetAccent)
            .copy(ledEffectEnabled = settings.ledEffectEnabled)
    }

    val finalBackground = if (isIncognitoActive) {
        Color(0xFF07090E)
    } else {
        effectiveColors.background
    }

    val toneSortedPhrases = remember(quickPhrases, inferredTone) {
        AppPhrasesRepository.getToneSortedPhrases(quickPhrases, inferredTone)
    }

    val toneBadgeLabel = remember(inferredTone) {
        if (inferredTone != InferredTone.NEUTRAL) "${inferredTone.emoji} ${inferredTone.label}" else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isIncognitoActive) {
                    Modifier.border(1.5.dp, effectiveColors.accentKeyBackground.copy(alpha = 0.55f))
                } else Modifier
            )
            .background(finalBackground)
            .navigationBarsPadding()
    ) {
        // Keep the top strip reserved in every mode so the keyboard does not jump vertically.
        if (mode != KeyboardMode.EMOJI && mode != KeyboardMode.CLIPBOARD && mode != KeyboardMode.TRANSLATE) {
            SuggestionBar(
                suggestions = if (settings.suggestionsEnabled) suggestions else emptyList(),
                quickPhrases = toneSortedPhrases,
                quickPhraseCategoryBadge = quickPhraseCategoryBadge,
                isQuickPhrasesMode = isQuickPhrasesMode,
                inferredToneLabel = toneBadgeLabel,
                isVoiceRecording = isVoiceRecording,
                currentLanguage = settings.language,
                colors = effectiveColors,
                isSecureFieldActive = isSecureFieldActive,
                isIncognitoActive = isIncognitoActive,
                onSuggestionClick = { word ->
                    onSuggestionSelected(word)
                    if (shiftState == ShiftState.SHIFTED) {
                        shiftState = ShiftState.OFF
                    }
                },
                onQuickPhraseClick = { phrase ->
                    onQuickPhraseSelected(phrase)
                    isQuickPhrasesMode = false
                },
                onToggleQuickPhrases = {
                    isQuickPhrasesMode = !isQuickPhrasesMode
                },
                onToggleIncognito = onToggleIncognito,
                onToggleVoiceInput = onToggleVoiceInput,
                onLanguageToggle = onLanguageSwitched,
                onOpenClipboard = {
                    mode = KeyboardMode.CLIPBOARD
                },
                onOpenTranslate = {
                    translationInputText = getSelectedTextForTranslation()
                    translationKeyboardMode = KeyboardMode.LETTERS
                    translationShiftState = ShiftState.OFF
                    mode = KeyboardMode.TRANSLATE
                },
                onOpenOcr = onOpenOcr,
                onOpenSettings = onOpenSettings,
                onToggleFloatingReader = onToggleFloatingReader
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth().height(44.dp))
        }

        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 90)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 70))) using
                    SizeTransform(clip = false) { _, _ -> tween(durationMillis = 0) }
            },
            label = "keyboardModeTransition"
        ) { currentMode ->
            when (currentMode) {
                KeyboardMode.EMOJI -> {
                    EmojiPickerView(
                        colors = effectiveColors,
                        onEmojiSelected = { emoji ->
                            onCharTyped(emoji)
                        },
                        onStickerSelected = { sticker ->
                            onStickerSelected(sticker)
                        },
                        onGifSelected = onGifSelected,
                        allowOnlineSearch = !isSecureFieldActive && !isIncognitoActive,
                        onBackspace = onBackspace,
                        onSwitchToAlphabet = {
                            mode = KeyboardMode.LETTERS
                        }
                    )
                }
                KeyboardMode.CLIPBOARD -> {
                    ClipboardHistoryView(
                        clips = clipboardClips,
                        colors = effectiveColors,
                        onClipSelected = { clipText ->
                            onCharTyped(clipText)
                            mode = KeyboardMode.LETTERS
                        },
                        onDeleteClip = onDeleteClip,
                        onClearAll = onClearAllClips,
                        onBackspace = onBackspace,
                        onSwitchToAlphabet = {
                            mode = KeyboardMode.LETTERS
                        }
                    )
                }
                KeyboardMode.TRANSLATE -> {
                    Column {
                        TranslationPanelView(
                            translationManager = translationManager,
                            colors = effectiveColors,
                            inputText = translationInputText,
                            onInputTextChange = { translationInputText = it },
                            onInsertText = { translatedText ->
                                onCharTyped(translatedText)
                                mode = KeyboardMode.LETTERS
                            },
                            onClose = {
                                mode = KeyboardMode.LETTERS
                            }
                        )
                        KeyboardLayout(
                            mode = translationKeyboardMode,
                            shiftState = translationShiftState,
                            imeAction = imeAction,
                            settings = settings.copy(showEmojiKey = false),
                            colors = effectiveColors,
                            onChar = { char ->
                                translationInputText += char
                                if (translationShiftState == ShiftState.SHIFTED) translationShiftState = ShiftState.OFF
                            },
                            onBackspace = {
                                if (translationInputText.isNotEmpty()) {
                                    val end = translationInputText.offsetByCodePoints(translationInputText.length, -1)
                                    translationInputText = translationInputText.substring(0, end)
                                }
                            },
                            onSpace = { translationInputText += " " },
                            onEnter = { translationInputText += "\n" },
                            onShiftToggle = {
                                translationShiftState = when (translationShiftState) {
                                    ShiftState.OFF -> ShiftState.SHIFTED
                                    ShiftState.SHIFTED -> ShiftState.CAPS_LOCKED
                                    ShiftState.CAPS_LOCKED -> ShiftState.OFF
                                }
                            },
                            onModeChange = { requestedMode ->
                                if (requestedMode == KeyboardMode.LETTERS || requestedMode == KeyboardMode.NUMBERS || requestedMode == KeyboardMode.SYMBOLS) {
                                    translationKeyboardMode = requestedMode
                                }
                            },
                            onLanguageToggle = {},
                            onHideKeyboard = { mode = KeyboardMode.LETTERS }
                        )
                    }
                }
                else -> {
                    KeyboardLayout(
                        mode = currentMode,
                        shiftState = shiftState,
                        imeAction = imeAction,
                        settings = settings,
                        colors = effectiveColors,
                        onChar = { char ->
                            onCharTyped(char)
                            if (shiftState == ShiftState.SHIFTED) {
                                shiftState = ShiftState.OFF
                            }
                        },
                        onBackspace = onBackspace,
                        onSpace = onSpace,
                        onEnter = onEnter,
                        onShiftToggle = {
                            shiftState = when (shiftState) {
                                ShiftState.OFF -> ShiftState.SHIFTED
                                ShiftState.SHIFTED -> ShiftState.CAPS_LOCKED
                                ShiftState.CAPS_LOCKED -> ShiftState.OFF
                            }
                        },
                        onModeChange = { newMode ->
                            mode = newMode
                        },
                        onLanguageToggle = onLanguageSwitched,
                        onHideKeyboard = onHideKeyboard
                    )
                }
            }
        }
    }
}
