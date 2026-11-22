package com.example.translation

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val flag: String
)

data class TranslationStatus(
    val isSourceDownloaded: Boolean = false,
    val isTargetDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressText: String = "",
    val error: String? = null
)

class TranslationManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val modelManager = RemoteModelManager.getInstance()

    private val _status = MutableStateFlow(TranslationStatus())
    val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private var activeTranslator: Translator? = null
    private var activePair: Pair<String, String>? = null
    private val translatorMutex = Mutex()
    @Volatile private var modelCheckGeneration = 0

    val supportedLanguages = listOf(
        SupportedLanguage(TranslateLanguage.PORTUGUESE, "Português", "🇧🇷"),
        SupportedLanguage(TranslateLanguage.ENGLISH, "English", "🇺🇸"),
        SupportedLanguage(TranslateLanguage.SPANISH, "Español", "🇪🇸"),
        SupportedLanguage(TranslateLanguage.FRENCH, "Français", "🇫🇷"),
        SupportedLanguage(TranslateLanguage.GERMAN, "Deutsch", "🇩🇪"),
        SupportedLanguage(TranslateLanguage.ITALIAN, "Italiano", "🇮🇹")
    )

    fun checkModelsDownloaded(sourceLang: String, targetLang: String) {
        val generation = synchronized(this) { ++modelCheckGeneration }
        _status.value = _status.value.copy(
            isSourceDownloaded = false,
            isTargetDownloaded = false,
            isDownloading = false,
            downloadProgressText = "",
            error = null
        )
        scope.launch {
            try {
                val sourceModel = TranslateRemoteModel.Builder(sourceLang).build()
                val targetModel = TranslateRemoteModel.Builder(targetLang).build()

                val isSourceDown = modelManager.isModelDownloaded(sourceModel).await()
                val isTargetDown = modelManager.isModelDownloaded(targetModel).await()

                if (generation == modelCheckGeneration) {
                    _status.value = _status.value.copy(
                        isSourceDownloaded = isSourceDown,
                        isTargetDownloaded = isTargetDown,
                        error = null
                    )
                }
            } catch (e: Exception) {
                if (generation == modelCheckGeneration) {
                    _status.value = _status.value.copy(error = e.localizedMessage ?: "Não foi possível verificar os modelos.")
                }
            }
        }
    }

    fun downloadModels(
        sourceLang: String,
        targetLang: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            _status.value = _status.value.copy(
                isDownloading = true,
                downloadProgressText = "Baixando modelos para tradução offline (requer internet apenas na 1ª vez)...",
                error = null
            )

            try {
                val conditions = DownloadConditions.Builder().build()
                val sourceModel = TranslateRemoteModel.Builder(sourceLang).build()
                val targetModel = TranslateRemoteModel.Builder(targetLang).build()

                // Download source model if needed
                val isSourceDown = modelManager.isModelDownloaded(sourceModel).await()
                if (!isSourceDown) {
                    _status.value = _status.value.copy(downloadProgressText = "Baixando pacote $sourceLang (≈30MB)...")
                    modelManager.download(sourceModel, conditions).await()
                }

                // Download target model if needed
                val isTargetDown = modelManager.isModelDownloaded(targetModel).await()
                if (!isTargetDown) {
                    _status.value = _status.value.copy(downloadProgressText = "Baixando pacote $targetLang (≈30MB)...")
                    modelManager.download(targetModel, conditions).await()
                }

                _status.value = _status.value.copy(
                    isSourceDownloaded = true,
                    isTargetDownloaded = true,
                    isDownloading = false,
                    downloadProgressText = "Modelos prontos para uso offline!",
                    error = null
                )
                onSuccess()
            } catch (e: Exception) {
                val msg = "Falha ao baixar modelo (verifique sua conexão com a internet): ${e.localizedMessage}"
                _status.value = _status.value.copy(
                    isDownloading = false,
                    downloadProgressText = "",
                    error = msg
                )
                onError(msg)
            }
        }
    }

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val clean = text.trim()
        if (clean.isEmpty()) return Result.success("")
        if (sourceLang == targetLang) return Result.success(clean)

        return translatorMutex.withLock {
            try {
                val pair = Pair(sourceLang, targetLang)
                if (activePair != pair || activeTranslator == null) {
                    activeTranslator?.close()
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLang)
                        .setTargetLanguage(targetLang)
                        .build()
                    activeTranslator = Translation.getClient(options)
                    activePair = pair
                }

                val translator = activeTranslator!!
                Result.success(translator.translate(clean).await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun close() {
        activeTranslator?.close()
        activeTranslator = null
    }

    companion object {
        @Volatile
        private var INSTANCE: TranslationManager? = null

        fun getInstance(context: Context): TranslationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TranslationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
