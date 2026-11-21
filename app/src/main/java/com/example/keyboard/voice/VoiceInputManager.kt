package com.example.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    data class Listening(val rms: Float = 0f) : VoiceRecognitionState()
    object Processing : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

class VoiceInputManager private constructor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _stateFlow = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val stateFlow: StateFlow<VoiceRecognitionState> = _stateFlow.asStateFlow()

    private val _recognizedTextFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val recognizedTextFlow: SharedFlow<String> = _recognizedTextFlow.asSharedFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        language: String = "pt-BR",
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit = {},
        onErrorCallback: (String) -> Unit = {}
    ) {
        if (!hasRecordAudioPermission()) {
            val errorMsg = "Permissão de microfone necessária. Conceda a permissão nas configurações do app."
            _stateFlow.value = VoiceRecognitionState.Error(errorMsg)
            mainHandler.post {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
            onErrorCallback(errorMsg)
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            val errorMsg = "Serviço de reconhecimento de voz não disponível no sistema."
            _stateFlow.value = VoiceRecognitionState.Error(errorMsg)
            mainHandler.post {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
            onErrorCallback(errorMsg)
            return
        }

        mainHandler.post {
            try {
                stopListeningInternal()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _stateFlow.value = VoiceRecognitionState.Listening(0f)
                        }

                        override fun onBeginningOfSpeech() {
                            _stateFlow.value = VoiceRecognitionState.Listening(0f)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val rms = (rmsdB / 10f).coerceIn(0f, 1f)
                            _rmsLevel.value = rms
                            _stateFlow.value = VoiceRecognitionState.Listening(rms)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _stateFlow.value = VoiceRecognitionState.Processing
                        }

                        override fun onError(error: Int) {
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Erro de gravação de áudio"
                                SpeechRecognizer.ERROR_CLIENT -> "Erro no cliente de voz"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária"
                                SpeechRecognizer.ERROR_NETWORK -> "Erro de rede para reconhecimento de voz"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tempo esgotado de rede"
                                SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma fala detectada. Tente falar mais perto."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor de voz ocupado"
                                SpeechRecognizer.ERROR_SERVER -> "Erro nos servidores de reconhecimento"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo de fala expirado"
                                else -> "Erro no reconhecimento de voz ($error)"
                            }
                            Log.w("VoiceInputManager", "Speech error ($error): $errorMsg")
                            _stateFlow.value = VoiceRecognitionState.Error(errorMsg)
                            mainHandler.post {
                                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                            onErrorCallback(errorMsg)
                        }

                        override fun onResults(results: Bundle?) {
                            _stateFlow.value = VoiceRecognitionState.Idle
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                _lastRecognizedText.value = text
                                _recognizedTextFlow.tryEmit(text)
                                onFinalResult(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                onPartialResult(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale para digitar...")
                }

                speechRecognizer?.startListening(intent)
                _stateFlow.value = VoiceRecognitionState.Listening(0f)
            } catch (e: Exception) {
                _stateFlow.value = VoiceRecognitionState.Error(e.localizedMessage ?: "Falha")
                Log.e("VoiceInputManager", "Failed to start listening", e)
                onErrorCallback(e.localizedMessage ?: "Falha ao iniciar reconhecimento de voz")
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("VoiceInputManager", "Error stopping recognizer", e)
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
            _stateFlow.value = VoiceRecognitionState.Idle
        }
    }

    fun destroy() {
        stopListening()
    }

    companion object {
        @Volatile
        private var instance: VoiceInputManager? = null

        fun getInstance(context: Context): VoiceInputManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceInputManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
