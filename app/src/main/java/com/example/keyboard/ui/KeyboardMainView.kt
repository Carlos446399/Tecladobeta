package com.example.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
    onOpenOcr: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val translationManager = remember(context) { TranslationManager.getInstance(context) }

    var mode by remember { mutableStateOf(KeyboardMode.LETTERS) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isQuickPhrasesMode by remember { mutableStateOf(false) }

    // Adaptive Chameleon Theme Resolution (overrides default theme if enabled and match exists)
    val effectiveColors = remember(settings.themeType, settings.accentColorType, settings.adaptiveThemeEnabled, activePackageName) {
        val adaptivePair = if (settings.adaptiveThemeEnabled) AppThemeMapper.resolve(activePackageName) else null
        val targetTheme = adaptivePair?.first ?: settings.themeType
        val targetAccent = adaptivePair?.second ?: settings.accentColorType
        KeyboardThemeColorsFactory.getColors(targetTheme, targetAccent)
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
        // Suggestion Bar (only shown when suggestions are enabled and not in emoji / clipboard / translate mode)
        if (settings.suggestionsEnabled && mode != KeyboardMode.EMOJI && mode != KeyboardMode.CLIPBOARD && mode != KeyboardMode.TRANSLATE) {
            SuggestionBar(
                suggestions = suggestions,
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
                    mode = KeyboardMode.TRANSLATE
                },
                onOpenOcr = onOpenOcr,
                onOpenSettings = onOpenSettings
            )
        }

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                    TranslationPanelView(
                        translationManager = translationManager,
                        colors = effectiveColors,
                        onInsertText = { translatedText ->
                            onCharTyped(translatedText)
                            mode = KeyboardMode.LETTERS
                        },
                        onClose = {
                            mode = KeyboardMode.LETTERS
                        }
                    )
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
