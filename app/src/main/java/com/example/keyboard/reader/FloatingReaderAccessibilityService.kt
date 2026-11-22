package com.example.keyboard.reader

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.hypot

/**
 * User-driven reader: it does not inspect accessibility events. It queries the node under
 * the floating control only during a drag and speaks it only after the user releases the control.
 */
class FloatingReaderAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var speechReady = false
    private var portugueseVoiceWarningShown = false
    private var pendingSpeech: String? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var downWindowX = 0
    private var downWindowY = 0
    private var touchSlopPx = 0f
    private var bubbleSizePx = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val fadeBubbleRunnable = Runnable { bubble?.alpha = IDLE_ALPHA }
    private val preferences by lazy {
        getSharedPreferences(FloatingReaderPreferences.FILE_NAME, MODE_PRIVATE)
    }
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == FloatingReaderPreferences.KEY_ENABLED) {
            mainHandler.post {
                if (FloatingReaderPreferences.isEnabled(this)) showBubble() else hideBubble()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        touchSlopPx = 8 * resources.displayMetrics.density
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        if (FloatingReaderPreferences.isEnabled(this)) showBubble()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not read, retain, or log accessibility events.
    }

    override fun onInterrupt() {
        textToSpeech?.stop()
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        hideBubble()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        ttsInitialized = true
        var selectedPortugueseVoice = false
        if (status == TextToSpeech.SUCCESS) {
            val engine = textToSpeech
            val portugueseLocales = listOf(
                Locale.forLanguageTag("pt-BR"),
                Locale.forLanguageTag("pt"),
                Locale.forLanguageTag("pt-PT")
            )
            for (locale in portugueseLocales) {
                val availability = engine?.setLanguage(locale) ?: TextToSpeech.LANG_MISSING_DATA
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    selectedPortugueseVoice = true
                    break
                }
            }
        }
        speechReady = status == TextToSpeech.SUCCESS && selectedPortugueseVoice

        if (!speechReady) {
            pendingSpeech = null
            showPortugueseVoiceUnavailableWarning()
            return
        }

        pendingSpeech?.let { speakNow(it) }
        pendingSpeech = null
    }

    private fun showBubble() {
        if (bubble != null) return
        val wm = windowManager ?: return
        if (textToSpeech == null) textToSpeech = TextToSpeech(applicationContext, this)

        val density = resources.displayMetrics.density
        bubbleSizePx = (56 * density).toInt()
        val view = TextView(this).apply {
            text = "Ler"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            contentDescription = "Leitor flutuante. Arraste sobre o texto e solte para ouvir."
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(37, 99, 235))
                setStroke((2 * density).toInt(), android.graphics.Color.WHITE)
            }
            elevation = 8 * density
            alpha = IDLE_ALPHA
            setOnTouchListener(::handleBubbleTouch)
        }
        val metrics = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (metrics.widthPixels - bubbleSizePx - (16 * density).toInt()).coerceAtLeast(0)
            y = (metrics.heightPixels * 0.35f).toInt().coerceAtLeast(0)
        }
        try {
            wm.addView(view, params)
            bubble = view
            bubbleParams = params
        } catch (_: Exception) {
            bubble = null
            bubbleParams = null
        }
    }

    private fun hideBubble() {
        bubble?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        bubble = null
        bubbleParams = null
        mainHandler.removeCallbacks(fadeBubbleRunnable)
        textToSpeech?.stop()
        pendingSpeech = null
    }

    private fun handleBubbleTouch(view: View, event: MotionEvent): Boolean {
        val params = bubbleParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mainHandler.removeCallbacks(fadeBubbleRunnable)
                view.alpha = 1f
                downRawX = event.rawX
                downRawY = event.rawY
                downWindowX = params.x
                downWindowY = params.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - downRawX
                val deltaY = event.rawY - downRawY
                if (hypot(deltaX, deltaY) > touchSlopPx) {
                    val metrics = resources.displayMetrics
                    params.x = (downWindowX + deltaX.toInt()).coerceIn(0, (metrics.widthPixels - bubbleSizePx).coerceAtLeast(0))
                    params.y = (downWindowY + deltaY.toInt()).coerceIn(0, (metrics.heightPixels - bubbleSizePx).coerceAtLeast(0))
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (_: Exception) {
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val text = findTextAt(event.rawX.toInt(), event.rawY.toInt())
                if (text.isNullOrBlank()) {
                    Toast.makeText(this, "Não encontrei texto acessível nesse ponto. Arraste sobre uma frase e solte.", Toast.LENGTH_SHORT).show()
                } else {
                    speak(text)
                }
                scheduleBubbleFade()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                scheduleBubbleFade()
                return true
            }
        }
        return true
    }

    private fun findTextAt(screenX: Int, screenY: Int): String? {
        val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
        val visibleWindows = try {
            windows
        } catch (_: Exception) {
            emptyList()
        }
        for (window in visibleWindows.sortedByDescending { it.layer }) {
            val bounds = Rect()
            window.getBoundsInScreen(bounds)
            val isAppWindowAtPoint = window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION &&
                bounds.contains(screenX, screenY)
            if (isAppWindowAtPoint) {
                try {
                    window.root?.let(candidateRoots::add)
                } catch (_: Exception) {
                }
            }
            @Suppress("DEPRECATION")
            window.recycle()
        }

        if (candidateRoots.isEmpty()) {
            val activeRoot = rootInActiveWindow
            if (activeRoot != null) {
                if (activeRoot.packageName?.toString() != packageName) {
                    candidateRoots.add(activeRoot)
                } else {
                    @Suppress("DEPRECATION")
                    activeRoot.recycle()
                }
            }
        }
        if (candidateRoots.isEmpty()) return null

        for (root in candidateRoots) {
            val foundText = findTextInRoot(root, screenX, screenY)
            if (!foundText.isNullOrBlank()) return foundText
        }
        return null
    }

    private fun findTextInRoot(root: AccessibilityNodeInfo, screenX: Int, screenY: Int): String? {
        var bestContainingText: String? = null
        var bestContainingDepth = -1
        var bestContainingArea = Int.MAX_VALUE
        var nearestText: String? = null
        var nearestDistance = Int.MAX_VALUE
        val maxNearDistance = (72 * resources.displayMetrics.density).toInt()

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (!node.isVisibleToUser || node.isPassword) return
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val text = (node.text?.toString()?.takeIf { it.isNotBlank() }
                ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() })
                ?.trim()
            if (text != null && !bounds.isEmpty) {
                val area = (bounds.width().toLong() * bounds.height().toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                if (bounds.contains(screenX, screenY)) {
                    if (depth > bestContainingDepth || (depth == bestContainingDepth && area < bestContainingArea)) {
                        bestContainingText = text.take(MAX_TEXT_LENGTH)
                        bestContainingDepth = depth
                        bestContainingArea = area
                    }
                } else {
                    val nearestX = screenX.coerceIn(bounds.left, bounds.right)
                    val nearestY = screenY.coerceIn(bounds.top, bounds.bottom)
                    val distance = hypot((screenX - nearestX).toDouble(), (screenY - nearestY).toDouble()).toInt()
                    if (distance <= maxNearDistance && distance < nearestDistance) {
                        nearestText = text.take(MAX_TEXT_LENGTH)
                        nearestDistance = distance
                    }
                }
            }

            for (index in 0 until node.childCount) {
                val child = try { node.getChild(index) } catch (_: Exception) { null } ?: continue
                try {
                    visit(child, depth + 1)
                } finally {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }

        try {
            visit(root, 0)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
        return (bestContainingText ?: nearestText)?.replace(Regex("\\s+"), " ")?.trim()
    }

    private fun scheduleBubbleFade() {
        mainHandler.removeCallbacks(fadeBubbleRunnable)
        mainHandler.postDelayed(fadeBubbleRunnable, FADE_DELAY_MS)
    }

    private fun speak(text: String) {
        val safeText = text.take(MAX_TEXT_LENGTH).trim()
        if (safeText.isBlank()) return
        if (!ttsInitialized) {
            pendingSpeech = safeText
            if (textToSpeech == null) textToSpeech = TextToSpeech(applicationContext, this)
            return
        }
        if (!speechReady) {
            showPortugueseVoiceUnavailableWarning()
            return
        }
        speakNow(safeText)
    }

    private fun showPortugueseVoiceUnavailableWarning() {
        if (portugueseVoiceWarningShown) return
        portugueseVoiceWarningShown = true
        mainHandler.post {
            Toast.makeText(
                applicationContext,
                "O mecanismo de voz não oferece Português (Brasil). Instale uma voz pt-BR nas configurações de Saída de texto para fala.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun speakNow(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "floating-reader-${SystemClock.uptimeMillis()}")
    }

    companion object {
        private const val IDLE_ALPHA = 0.35f
        private const val FADE_DELAY_MS = 1200L
        private const val MAX_TEXT_LENGTH = 1200
    }
}
