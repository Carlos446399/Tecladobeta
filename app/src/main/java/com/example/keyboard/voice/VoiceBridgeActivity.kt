package com.example.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Transparent activity that requests microphone permissions or acts as a speech recognition
 * bridge when called from CustomInputMethodService.
 *
 * If permissions are granted and the device supports the system RecognizerIntent prompt dialog,
 * it runs the recognizer dialog and returns the text directly to the IME via VoiceBridge.
 */
class VoiceBridgeActivity : ComponentActivity() {

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            if (recognizedText.isNotEmpty()) {
                VoiceBridge.postRecognizedText(recognizedText)
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permissão concedida! Toque no microfone novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissão de microfone necessária para digitar por voz.", Toast.LENGTH_LONG).show()
        }
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasAudio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val startSpeechDialog = intent.getBooleanExtra(EXTRA_START_SPEECH_DIALOG, false)
        val languageTag = intent.getStringExtra(EXTRA_LANGUAGE_TAG) ?: "pt-BR"

        if (!hasAudio) {
            // Request permission directly with Android system permission dialog
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (startSpeechDialog) {
            // Launch the system speech recognizer dialog
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale agora para digitar...")
            }
            try {
                speechLauncher.launch(speechIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Reconhecimento de voz indisponível: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
                overridePendingTransition(0, 0)
            }
        } else {
            // Already has permission, nothing more to do
            finish()
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_START_SPEECH_DIALOG = "extra_start_speech_dialog"
        const val EXTRA_LANGUAGE_TAG = "extra_language_tag"

        fun startForPermission(context: Context) {
            val intent = Intent(context, VoiceBridgeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_START_SPEECH_DIALOG, false)
            }
            context.startActivity(intent)
        }

        fun startForSpeechDialog(context: Context, languageTag: String) {
            val intent = Intent(context, VoiceBridgeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_START_SPEECH_DIALOG, true)
                putExtra(EXTRA_LANGUAGE_TAG, languageTag)
            }
            context.startActivity(intent)
        }
    }
}

object VoiceBridge {
    private val _voiceFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val voiceFlow = _voiceFlow.asSharedFlow()

    fun postRecognizedText(text: String) {
        _voiceFlow.tryEmit(text)
    }
}
