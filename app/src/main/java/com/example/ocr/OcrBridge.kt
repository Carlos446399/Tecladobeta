package com.example.ocr

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared bridge between the lightweight OCR Scanner Activity and the InputMethodService.
 * When text is captured and recognized via on-device ML Kit, it is emitted here
 * so CustomInputMethodService can insert it into the active editor field.
 */
object OcrBridge {
    private val _ocrTextFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val ocrTextFlow: SharedFlow<String> = _ocrTextFlow.asSharedFlow()

    fun sendOcrText(text: String) {
        val clean = text.trim()
        if (clean.isNotEmpty()) {
            _ocrTextFlow.tryEmit(clean)
        }
    }
}
