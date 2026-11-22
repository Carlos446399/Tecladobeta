package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.keyboard.data.AppPhrasesRepository
import com.example.keyboard.data.ClipboardHistoryRepository
import com.example.keyboard.data.InferredTone
import com.example.keyboard.data.GraphemeDeletion
import com.example.keyboard.data.KeyboardSettingsRepository
import com.example.keyboard.data.KeyboardStatsRepository
import com.example.keyboard.data.TextShortcutRepository
import com.example.keyboard.data.UnknownWordLearningTracker
import com.example.keyboard.data.UserDictionaryRepository
import com.example.keyboard.data.WordDictionary
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.reader.FloatingReaderPreferences
import com.example.keyboard.ui.KeyboardMainView
import com.example.keyboard.voice.VoiceBridge
import com.example.keyboard.voice.VoiceBridgeActivity
import com.example.keyboard.voice.VoiceInputManager
import com.example.keyboard.voice.VoiceRecognitionState
import com.example.ocr.OcrBridge
import com.example.ocr.OcrScannerActivity
import com.example.stickers.GiphyGifHelper
import com.example.stickers.GiphyMediaItem
import com.example.stickers.StickerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var repository: KeyboardSettingsRepository
    private lateinit var statsRepository: KeyboardStatsRepository
    private lateinit var clipboardRepository: ClipboardHistoryRepository
    private lateinit var textShortcutRepository: TextShortcutRepository
    private lateinit var appPhrasesRepository: AppPhrasesRepository
    private lateinit var userDictionaryRepository: UserDictionaryRepository
    private lateinit var voiceInputManager: VoiceInputManager

    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var clipboardManager: ClipboardManager? = null
    private var isClipListenerRegistered = false

    private val currentWordBuffer = StringBuilder()
    private val unknownWordLearningTracker = UnknownWordLearningTracker()
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private var lastSuggestionQuery: String? = null

    private val _quickPhrases = MutableStateFlow<List<String>>(emptyList())
    val quickPhrases = _quickPhrases.asStateFlow()

    private val _quickPhraseBadge = MutableStateFlow("Geral")
    val quickPhraseBadge = _quickPhraseBadge.asStateFlow()

    private val _activePackageName = MutableStateFlow<String?>(null)
    val activePackageName = _activePackageName.asStateFlow()

    private val _inferredTone = MutableStateFlow(InferredTone.NEUTRAL)
    val inferredTone = _inferredTone.asStateFlow()

    private val _isSecureFieldActive = MutableStateFlow(false)
    val isSecureFieldActive = _isSecureFieldActive.asStateFlow()

    private val _isIncognitoModeActive = MutableStateFlow(false)
    val isIncognitoModeActive = _isIncognitoModeActive.asStateFlow()

    private val shouldSuppressDataCollection: Boolean
        get() = _isSecureFieldActive.value || _isIncognitoModeActive.value

    private var currentEditorInfo: EditorInfo? = null
    private var phrasesJob: Job? = null
    private var voiceCollectorJob: Job? = null

    // Autocorrect Undo Record
    private data class AutocorrectUndoState(
        val originalWord: String,
        val committedText: String
    )
    private var lastAutocorrectUndo: AutocorrectUndoState? = null

    // History for automatic language detection (last 5 words)
    private val recentWordsHistory = ArrayDeque<String>(6)

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!shouldSuppressDataCollection) {
            try {
                val clip = clipboardManager?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.coerceToText(this@CustomInputMethodService)?.toString()
                    if (!text.isNullOrBlank()) {
                        clipboardRepository.addClip(text, isSecure = false)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        repository = KeyboardSettingsRepository.getInstance(this)
        statsRepository = KeyboardStatsRepository.getInstance(this)
        clipboardRepository = ClipboardHistoryRepository.getInstance(this)
        textShortcutRepository = TextShortcutRepository.getInstance(this)
        appPhrasesRepository = AppPhrasesRepository.getInstance(this)
        userDictionaryRepository = UserDictionaryRepository.getInstance(this)
        
        // Initialize dictionary asynchronously in background thread to prevent ANR / startup UI freeze
        lifecycleScope.launch(Dispatchers.IO) {
            WordDictionary.init(applicationContext)
            withContext(Dispatchers.Main) {
                lastSuggestionQuery = null
                updateSuggestions(currentWordBuffer.toString())
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        voiceInputManager = VoiceInputManager.getInstance(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        updateSuggestions("")

        // Listen for recognized text from OCR Camera Scanner
        lifecycleScope.launch {
            OcrBridge.ocrTextFlow.collect { scannedText ->
                if (scannedText.isNotBlank()) {
                    currentInputConnection?.commitText(scannedText, 1)
                    statsRepository.recordTypedWord(
                        scannedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                        isSecure = _isSecureFieldActive.value
                    )
                    syncWordFromCursor()
                }
            }
        }

        // Listen for recognized text from Voice Input (in-IME recognizer)
        voiceCollectorJob?.cancel()
        voiceCollectorJob = lifecycleScope.launch {
            voiceInputManager.recognizedTextFlow.collect { voiceText ->
                if (voiceText.isNotBlank()) {
                    currentInputConnection?.commitText("$voiceText ", 1)
                    statsRepository.recordTypedWord(
                        voiceText.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                        isSecure = shouldSuppressDataCollection
                    )
                    syncWordFromCursor()
                }
            }
        }

        // Listen for recognized text from VoiceBridge (fallback system speech dialog)
        lifecycleScope.launch {
            VoiceBridge.voiceFlow.collect { voiceText ->
                if (voiceText.isNotBlank()) {
                    currentInputConnection?.commitText("$voiceText ", 1)
                    statsRepository.recordTypedWord(
                        voiceText.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                        isSecure = shouldSuppressDataCollection
                    )
                    syncWordFromCursor()
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        installViewTreeOwners()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        currentEditorInfo = info

        val isSecure = isPasswordField(info)
        _isSecureFieldActive.value = isSecure
        lastAutocorrectUndo = null

        val packageName = info?.packageName
        _activePackageName.value = packageName
        val category = AppPhrasesRepository.getCategoryForPackage(packageName)
        _quickPhraseBadge.value = when (category) {
            AppPhrasesRepository.CATEGORY_MESSAGING -> "Chat"
            AppPhrasesRepository.CATEGORY_EMAIL -> "E-mail"
            else -> "Geral"
        }

        phrasesJob?.cancel()
        phrasesJob = lifecycleScope.launch {
            appPhrasesRepository.getPhrasesForPackageFlow(packageName).collect { list ->
                _quickPhrases.value = list
            }
        }

        if (!isClipListenerRegistered) {
            try {
                clipboardManager?.addPrimaryClipChangedListener(clipListener)
                isClipListenerRegistered = true
            } catch (_: Exception) {
            }
        }

        if (!shouldSuppressDataCollection) {
            try {
                val clip = clipboardManager?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.coerceToText(this@CustomInputMethodService)?.toString()
                    if (!text.isNullOrBlank()) {
                        clipboardRepository.addClip(text, isSecure = false)
                    }
                }
            } catch (_: Exception) {
            }
        }

        syncWordFromCursor()
    }

    private fun isPasswordField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val isTextPassword = (inputType and EditorInfo.TYPE_MASK_CLASS == EditorInfo.TYPE_CLASS_TEXT) &&
            (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
             variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
             variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        val isNumberPassword = (inputType and EditorInfo.TYPE_MASK_CLASS == EditorInfo.TYPE_CLASS_NUMBER) &&
            (variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
        return isTextPassword || isNumberPassword
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        syncWordFromCursor()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        currentWordBuffer.clear()
        lastAutocorrectUndo = null
        phrasesJob?.cancel()
        voiceInputManager.stopListening()

        if (isClipListenerRegistered) {
            try {
                clipboardManager?.removePrimaryClipChangedListener(clipListener)
                isClipListenerRegistered = false
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        voiceCollectorJob?.cancel()
        voiceInputManager.destroy()
        if (isClipListenerRegistered) {
            try {
                clipboardManager?.removePrimaryClipChangedListener(clipListener)
                isClipListenerRegistered = false
            } catch (_: Exception) {
            }
        }
    }

    override fun onEvaluateInputViewShown(): Boolean = true
    override fun onEvaluateFullscreenMode(): Boolean = false
    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean = true

    override fun onConfigureWindow(win: android.view.Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        win.decorView.setViewTreeLifecycleOwner(this)
        win.decorView.setViewTreeViewModelStoreOwner(this)
        win.decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    private fun installViewTreeOwners() {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onCreateInputView(): View {
        installViewTreeOwners()
        _isIncognitoModeActive.value = false

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@CustomInputMethodService)
            setViewTreeViewModelStoreOwner(this@CustomInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@CustomInputMethodService)

            setContent {
                val currentSettings by repository.settings.collectAsStateWithLifecycle()
                val suggestionsList by suggestions.collectAsStateWithLifecycle()
                val phrasesList by quickPhrases.collectAsStateWithLifecycle()
                val phraseBadge by quickPhraseBadge.collectAsStateWithLifecycle()
                val currentPackageName by activePackageName.collectAsStateWithLifecycle()
                val currentTone by inferredTone.collectAsStateWithLifecycle()
                val isSecureActive by isSecureFieldActive.collectAsStateWithLifecycle()
                val isIncognitoActive by isIncognitoModeActive.collectAsStateWithLifecycle()
                val voiceState by voiceInputManager.stateFlow.collectAsStateWithLifecycle()
                val isVoiceActive = voiceState is VoiceRecognitionState.Listening
                val recentClips by clipboardRepository.recentClips.collectAsStateWithLifecycle(emptyList())

                KeyboardMainView(
                    settings = currentSettings,
                    imeAction = currentEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED,
                    suggestions = suggestionsList,
                    quickPhrases = phrasesList,
                    quickPhraseCategoryBadge = phraseBadge,
                    activePackageName = currentPackageName,
                    inferredTone = currentTone,
                    isVoiceRecording = isVoiceActive,
                    isSecureFieldActive = isSecureActive,
                    isIncognitoActive = isIncognitoActive,
                    clipboardClips = recentClips,
                    onCharTyped = { char ->
                        handleChar(char)
                    },
                    onBackspace = {
                        handleBackspace()
                    },
                    onSpace = {
                        handleSpace()
                    },
                    onEnter = {
                        handleEnter()
                    },
                    onSuggestionSelected = { word ->
                        handleSuggestionSelected(word)
                    },
                    onQuickPhraseSelected = { phrase ->
                        handleQuickPhraseSelected(phrase)
                    },
                    onToggleIncognito = {
                        _isIncognitoModeActive.value = !_isIncognitoModeActive.value
                        syncWordFromCursor()
                    },
                    onToggleVoiceInput = {
                        handleToggleVoiceInput()
                    },
                    onLanguageSwitched = {
                        handleLanguageSwitch()
                    },
                    onDeleteClip = { clipId ->
                        clipboardRepository.deleteClip(clipId)
                    },
                    onClearAllClips = {
                        clipboardRepository.clearAll()
                    },
                    onStickerSelected = { sticker ->
                        StickerHelper.sendSticker(
                            context = this@CustomInputMethodService,
                            sticker = sticker,
                            inputConnection = currentInputConnection,
                            editorInfo = currentEditorInfo
                        ) { feedback ->
                            Toast.makeText(this@CustomInputMethodService, feedback, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onGifSelected = { item: GiphyMediaItem ->
                        lifecycleScope.launch {
                            try {
                                val feedback = GiphyGifHelper.send(
                                    context = this@CustomInputMethodService,
                                    item = item,
                                    inputConnection = currentInputConnection,
                                    editorInfo = currentEditorInfo
                                )
                                Toast.makeText(this@CustomInputMethodService, feedback, Toast.LENGTH_SHORT).show()
                            } catch (exception: Exception) {
                                Toast.makeText(
                                    this@CustomInputMethodService,
                                    exception.message ?: "Não foi possível enviar este GIF.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onOpenOcr = {
                        val intent = Intent(this@CustomInputMethodService, OcrScannerActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    },
                    onOpenSettings = {
                        val intent = Intent(this@CustomInputMethodService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    },
                    onToggleFloatingReader = { enabled ->
                        FloatingReaderPreferences.setEnabled(this@CustomInputMethodService, enabled)
                        if (enabled && !FloatingReaderPreferences.isAccessibilityServiceEnabled(this@CustomInputMethodService)) {
                            Toast.makeText(
                                this@CustomInputMethodService,
                                "Ative o Serviço de Acessibilidade do Leitor flutuante nas configurações do Android.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onHideKeyboard = {
                        requestHideSelf(0)
                    },
                    getSelectedTextForTranslation = {
                        if (shouldSuppressDataCollection) "" else {
                            currentInputConnection?.getSelectedText(0)?.toString()?.take(2000).orEmpty()
                        }
                    }
                )
            }
        }
    }

    private fun handleToggleVoiceInput() {
        triggerFeedback()
        val currentState = voiceInputManager.stateFlow.value
        if (currentState is VoiceRecognitionState.Listening) {
            voiceInputManager.stopListening()
            Toast.makeText(this, "Digitação por voz pausada", Toast.LENGTH_SHORT).show()
            return
        }

        if (!voiceInputManager.hasRecordAudioPermission()) {
            Toast.makeText(
                this,
                "Permissão de microfone necessária. Conceda o acesso para falar.",
                Toast.LENGTH_SHORT
            ).show()
            VoiceBridgeActivity.startForPermission(this)
            return
        }

        val langTag = if (repository.settings.value.language == KeyboardLanguage.PT_BR) "pt-BR" else "en-US"

        if (!voiceInputManager.isSpeechRecognitionAvailable()) {
            // SpeechRecognizer service not available directly in background IME; fallback to system speech dialog
            VoiceBridgeActivity.startForSpeechDialog(this, langTag)
            return
        }

        Toast.makeText(this, "🎙️ Ouvindo... Fale para digitar", Toast.LENGTH_SHORT).show()
        voiceInputManager.startListening(
            language = langTag,
            onErrorCallback = { error ->
                // If in-IME speech fails due to network/client/busy, attempt system dialog fallback
                VoiceBridgeActivity.startForSpeechDialog(this@CustomInputMethodService, langTag)
            }
        )
    }

    private fun syncWordFromCursor() {
        val ic = currentInputConnection ?: return

        // Check if there is actively selected text (word or phrase)
        val selectedText = ic.getSelectedText(0)?.toString()?.trim() ?: ""
        if (selectedText.isNotEmpty() && !shouldSuppressDataCollection) {
            currentWordBuffer.clear()
            currentWordBuffer.append(selectedText)
            updateSuggestionsForSelectedText(selectedText)
            return
        }

        val textBefore = ic.getTextBeforeCursor(120, 0)?.toString() ?: ""
        val word = textBefore.takeLastWhile { it.isLetterOrDigit() }
        currentWordBuffer.clear()
        currentWordBuffer.append(word)

        if (!shouldSuppressDataCollection && repository.settings.value.suggestionsEnabled) {
            val previousWord = textBefore.dropLast(word.length).trimEnd()
                .takeLastWhile { it.isLetterOrDigit() || it == '\'' || it == '’' }
                .ifBlank { null }
            updateSuggestions(word, previousWord)
            _inferredTone.value = AppPhrasesRepository.inferTone(textBefore)
        } else {
            _suggestions.value = emptyList()
            lastSuggestionQuery = null
            _inferredTone.value = InferredTone.NEUTRAL
        }
    }

    private fun updateSuggestionsForSelectedText(selectedText: String) {
        if (shouldSuppressDataCollection) {
            _suggestions.value = emptyList()
            return
        }
        val currentLang = repository.settings.value.language
        val list = mutableListOf<String>()

        // Check phrase or word correction
        val phraseCorrection = WordDictionary.getPhraseOrWordCorrection(selectedText, currentLang)
        if (phraseCorrection != null && !phraseCorrection.equals(selectedText, ignoreCase = true)) {
            list.add(phraseCorrection)
        }

        // If single word, add regular suggestions and autocorrect candidate
        if (!selectedText.contains(" ")) {
            val autoCorrect = WordDictionary.getAutocorrectWord(selectedText, currentLang)
            if (autoCorrect != null && !list.contains(autoCorrect)) {
                list.add(autoCorrect)
            }
            val dictSuggestions = WordDictionary.getSuggestions(selectedText, currentLang)
            for (s in dictSuggestions) {
                if (!list.contains(s)) {
                    list.add(s)
                }
            }
        }

        _suggestions.value = list
    }

    private fun handleChar(char: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return

        // Multi-character insertions (e.g. from Clipboard, Translation, Emoji combinations)
        if (char.length > 1) {
            lastAutocorrectUndo = null
            ic.commitText(char, 1)

            val wordCount = char.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
            statsRepository.recordTypedWord(wordCount, isSecure = shouldSuppressDataCollection)
            syncWordFromCursor()
            return
        }

        // If typing punctuation and we have a word in buffer, check text expansion / autocorrect
        val isPunctuation = char in listOf(".", ",", "!", "?", ";", ":")
        if (isPunctuation && currentWordBuffer.isNotEmpty() && !shouldSuppressDataCollection) {
            processWordEndAndCommit(char)
            return
        }

        lastAutocorrectUndo = null
        ic.commitText(char, 1)

        statsRepository.recordTypedChar(char, isSecure = shouldSuppressDataCollection)

        syncWordFromCursor()
    }

    private fun handleBackspace() {
        triggerFeedback()
        val ic = currentInputConnection ?: return

        // 1. Check if user just had an autocorrection / text expansion: revert it to original!
        val undoState = lastAutocorrectUndo
        if (undoState != null) {
            lastAutocorrectUndo = null
            // Delete the committed autocorrected text and separator
            val committedLength = undoState.committedText.length
            ic.deleteSurroundingText(committedLength, 0)
            // Revert back to original typed text
            ic.commitText(undoState.originalWord, 1)

            currentWordBuffer.clear()
            currentWordBuffer.append(undoState.originalWord)
            updateSuggestions(undoState.originalWord)
            return
        }

        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            val before = ic.getTextBeforeCursor(128, 0)?.toString().orEmpty()
            if (before.isEmpty()) {
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
            } else {
                val graphemeLength = GraphemeDeletion.previousClusterLength(before)
                if (graphemeLength > 0) {
                    ic.deleteSurroundingText(graphemeLength, 0)
                } else {
                    sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
                }
            }
        }

        syncWordFromCursor()
    }

    private fun handleSpace() {
        triggerFeedback()
        processWordEndAndCommit(" ")
    }

    private fun processWordEndAndCommit(separator: String) {
        val ic = currentInputConnection ?: return
        val settings = repository.settings.value
        
        var rawWord = currentWordBuffer.toString().trim()
        if (rawWord.isEmpty()) {
            val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
            rawWord = textBefore.takeLastWhile { it.isLetterOrDigit() }
        }

        if (!shouldSuppressDataCollection && rawWord.isNotEmpty()) {
            // 1. Check Text Expansion Shortcuts (e.g. "vlw" -> "Valeu, muito obrigado!")
            val expansion = textShortcutRepository.getExpansion(rawWord)
            if (expansion != null) {
                ic.deleteSurroundingText(rawWord.length, 0)
                val committed = "$expansion$separator"
                ic.commitText(committed, 1)
                lastAutocorrectUndo = AutocorrectUndoState(originalWord = rawWord, committedText = committed)

                recordWordComplete(expansion)
                currentWordBuffer.clear()
                updateSuggestions("")
                return
            }

            if (learnUnknownWordIfConfirmed(rawWord, settings.language)) {
                recordWordComplete(rawWord)
                lastAutocorrectUndo = null
                ic.commitText(separator, 1)
                currentWordBuffer.clear()
                updateSuggestions("")
                return
            }

            // 2. Check Autocorrect (Word dictionary + typo correction)
            val shouldAutoCorrect = if (separator == " ") settings.spaceAutoCorrectEnabled else true
            if (settings.autoCorrectEnabled && shouldAutoCorrect) {
                val corrected = WordDictionary.getAutocorrectWord(rawWord, settings.language)
                if (corrected != null && corrected != rawWord) {
                    ic.deleteSurroundingText(rawWord.length, 0)
                    val committed = "$corrected$separator"
                    ic.commitText(committed, 1)
                    lastAutocorrectUndo = AutocorrectUndoState(originalWord = rawWord, committedText = committed)

                    recordWordComplete(corrected)
                    currentWordBuffer.clear()
                    updateSuggestions("")
                    return
                }
            }

            // Normal word commitment without correction
            recordWordComplete(rawWord)
        }

        // Standard separator commit
        lastAutocorrectUndo = null
        ic.commitText(separator, 1)
        currentWordBuffer.clear()
        if (!shouldSuppressDataCollection) {
            updateSuggestions("")
        }
    }

    private fun recordWordComplete(word: String) {
        statsRepository.recordTypedWord(1, isSecure = shouldSuppressDataCollection)

        // Automatic Language Detection Check
        if (word.length >= 2 && !shouldSuppressDataCollection) {
            if (recentWordsHistory.size >= 5) {
                recentWordsHistory.removeFirst()
            }
            recentWordsHistory.addLast(word)

            if (repository.settings.value.autoLanguageDetectionEnabled) {
                val detected = WordDictionary.detectLanguageFromWords(recentWordsHistory.toList())
                if (detected != null && detected != repository.settings.value.language) {
                    repository.setLanguage(detected)
                }
            }
        }
    }

    private fun learnUnknownWordIfConfirmed(word: String, language: KeyboardLanguage): Boolean {
        if (word.isBlank()) return false
        val known = WordDictionary.isWordInDictionary(word, language)
        val learnNow = unknownWordLearningTracker.recordAttempt(
            word = word,
            isKnown = known,
            isSuppressed = shouldSuppressDataCollection || !WordDictionary.isInitialized
        )
        if (!learnNow) return false

        // Add to the in-memory index immediately so the next keystroke cannot autocorrect it
        // while Room persists the same word asynchronously.
        WordDictionary.addUserCustomWord(word)
        userDictionaryRepository.addWord(word, language.code.lowercase())
        return true
    }

    private fun handleEnter() {
        triggerFeedback()
        val ic = currentInputConnection ?: return
        val action = currentEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE
        val actionId = action and EditorInfo.IME_MASK_ACTION

        lastAutocorrectUndo = null
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            sendKeyChar('\n')
        }

        if (currentWordBuffer.isNotEmpty()) {
            val finishedWord = currentWordBuffer.toString()
            learnUnknownWordIfConfirmed(finishedWord, repository.settings.value.language)
            recordWordComplete(finishedWord)
        }
        currentWordBuffer.clear()
        if (!shouldSuppressDataCollection) {
            updateSuggestions("")
        }
    }

    private fun handleSuggestionSelected(word: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return

        lastAutocorrectUndo = null
        val selectedText = ic.getSelectedText(0)?.toString() ?: ""
        if (selectedText.isNotEmpty()) {
            ic.commitText(word, 1)
        } else {
            val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
            val wordLength = textBefore.takeLastWhile { it.isLetterOrDigit() }.length
            if (wordLength > 0) {
                ic.deleteSurroundingText(wordLength, 0)
            }
            ic.commitText("$word ", 1)
        }

        recordWordComplete(word)
        currentWordBuffer.clear()
        if (!shouldSuppressDataCollection) {
            updateSuggestions("")
        }
    }

    private fun handleQuickPhraseSelected(phrase: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return
        lastAutocorrectUndo = null

        // Insert the phrase followed by a space
        ic.commitText("$phrase ", 1)
        recordWordComplete(phrase)
        currentWordBuffer.clear()
        if (!shouldSuppressDataCollection) {
            updateSuggestions("")
        }
    }

    private fun handleLanguageSwitch() {
        triggerFeedback()
        val currentLang = repository.settings.value.language
        val nextLang = if (currentLang == KeyboardLanguage.PT_BR) {
            KeyboardLanguage.EN_US
        } else {
            KeyboardLanguage.PT_BR
        }
        repository.setLanguage(nextLang)
        if (!shouldSuppressDataCollection) {
            updateSuggestions(currentWordBuffer.toString())
        }
    }

    private fun updateSuggestions(prefix: String) {
        updateSuggestions(prefix, null)
    }

    private fun updateSuggestions(prefix: String, previousWord: String?) {
        if (shouldSuppressDataCollection) {
            _suggestions.value = emptyList()
            lastSuggestionQuery = null
            return
        }
        if (!repository.settings.value.suggestionsEnabled) {
            _suggestions.value = emptyList()
            lastSuggestionQuery = null
            return
        }
        val currentLang = repository.settings.value.language
        val contextWord = previousWord ?: if (prefix.isBlank()) {
            currentInputConnection?.getTextBeforeCursor(120, 0)?.toString()?.trimEnd()
                ?.takeLastWhile { it.isLetterOrDigit() || it == '\'' || it == '’' }
                ?.ifBlank { null }
        } else null
        val queryKey = "${currentLang.code}|${prefix.lowercase()}|${contextWord.orEmpty().lowercase()}"
        if (queryKey == lastSuggestionQuery) return
        lastSuggestionQuery = queryKey
        _suggestions.value = WordDictionary.getSuggestions(prefix, currentLang, previousWord = contextWord)
    }

    private fun triggerFeedback() {
        val settings = repository.settings.value

        // Haptic Feedback
        if (settings.hapticEnabled && vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(
                            settings.vibrationDurationMs.coerceIn(5L, 100L),
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(settings.vibrationDurationMs.coerceIn(5L, 100L))
                }
            } catch (_: Exception) {
            }
        }

        // Sound Feedback
        if (settings.soundEnabled && audioManager != null) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
            } catch (_: Exception) {
            }
        }
    }
}
