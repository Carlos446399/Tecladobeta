package com.example.keyboard.ui

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardMode
import com.example.keyboard.model.KeyboardSettings
import com.example.keyboard.model.KeyboardThemeColors
import com.example.keyboard.model.ShiftState

@Composable
fun KeyboardLayout(
    mode: KeyboardMode,
    shiftState: ShiftState,
    imeAction: Int,
    settings: KeyboardSettings,
    colors: KeyboardThemeColors,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onShiftToggle: () -> Unit,
    onModeChange: (KeyboardMode) -> Unit,
    onLanguageToggle: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyHeight = settings.keySizeMode.heightDp.dp
    val shapeStyle = settings.keyShapeStyle
    val hGap = shapeStyle.hGapDp.dp
    val vGap = shapeStyle.vGapDp.dp
    val cornerRadius = shapeStyle.cornerRadiusDp.dp
    val fontScale = settings.keySizeMode.fontScale

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        when (mode) {
            KeyboardMode.LETTERS -> {
                AlphabetLayout(
                    shiftState = shiftState,
                    language = settings.language,
                    showEmojiKey = settings.showEmojiKey,
                    showNumberRow = settings.showNumberRow,
                    imeAction = imeAction,
                    colors = colors,
                    keyHeight = keyHeight,
                    hGap = hGap,
                    vGap = vGap,
                    cornerRadius = cornerRadius,
                    fontScale = fontScale,
                    onChar = onChar,
                    onBackspace = onBackspace,
                    onSpace = onSpace,
                    onEnter = onEnter,
                    onShiftToggle = onShiftToggle,
                    onModeChange = onModeChange,
                    onLanguageToggle = onLanguageToggle
                )
            }
            KeyboardMode.NUMBERS -> {
                NumbersLayout(
                    showEmojiKey = settings.showEmojiKey,
                    imeAction = imeAction,
                    colors = colors,
                    keyHeight = keyHeight,
                    hGap = hGap,
                    vGap = vGap,
                    cornerRadius = cornerRadius,
                    fontScale = fontScale,
                    onChar = onChar,
                    onBackspace = onBackspace,
                    onSpace = onSpace,
                    onEnter = onEnter,
                    onModeChange = onModeChange
                )
            }
            KeyboardMode.SYMBOLS -> {
                SymbolsLayout(
                    showEmojiKey = settings.showEmojiKey,
                    imeAction = imeAction,
                    colors = colors,
                    keyHeight = keyHeight,
                    hGap = hGap,
                    vGap = vGap,
                    cornerRadius = cornerRadius,
                    fontScale = fontScale,
                    onChar = onChar,
                    onBackspace = onBackspace,
                    onSpace = onSpace,
                    onEnter = onEnter,
                    onModeChange = onModeChange
                )
            }
            KeyboardMode.EMOJI, KeyboardMode.CLIPBOARD, KeyboardMode.TRANSLATE -> {
                // Handled in parent composables
            }
        }
    }
}

@Composable
private fun AlphabetLayout(
    shiftState: ShiftState,
    language: KeyboardLanguage,
    showEmojiKey: Boolean,
    showNumberRow: Boolean,
    imeAction: Int,
    colors: KeyboardThemeColors,
    keyHeight: androidx.compose.ui.unit.Dp,
    hGap: androidx.compose.ui.unit.Dp,
    vGap: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    fontScale: Float,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onShiftToggle: () -> Unit,
    onModeChange: (KeyboardMode) -> Unit,
    onLanguageToggle: () -> Unit
) {
    val isShifted = shiftState != ShiftState.OFF

    // Number Row (when enabled)
    val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    // Row 1
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row1Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    // Row 2
    val row2 = if (language == KeyboardLanguage.PT_BR) {
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ç")
    } else {
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    }

    // Row 3
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    // Accents map for long press
    val accentsMap = mapOf(
        "a" to listOf("á", "à", "ã", "â", "ä"),
        "e" to listOf("3", "é", "ê", "è", "ë"),
        "i" to listOf("8", "í", "ì", "î", "ï"),
        "o" to listOf("9", "ó", "ô", "õ", "ò", "ö"),
        "u" to listOf("7", "ú", "ù", "û", "ü"),
        "c" to listOf("ç")
    )

    // Dedicated Fixed Number Row Rendering
    if (showNumberRow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            numberRow.forEach { digit ->
                KeyboardKey(
                    modifier = Modifier.weight(1f),
                    text = digit,
                    colors = colors,
                    height = keyHeight,
                    horizontalGap = hGap,
                    verticalGap = vGap,
                    cornerRadius = cornerRadius,
                    fontScale = fontScale,
                    onKeyPress = { onChar(digit) }
                )
            }
        }
    }

    // Row 1 Rendering
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        row1.forEachIndexed { index, char ->
            val displayChar = if (isShifted) char.uppercase() else char
            val digit = if (showNumberRow) null else row1Digits.getOrNull(index)
            val accents = accentsMap[char]?.map { if (isShifted && it.firstOrNull()?.isLetter() == true) it.uppercase() else it } ?: emptyList()

            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = displayChar,
                secondaryText = digit,
                accents = accents,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(displayChar) },
                onLongPress = if (digit != null && accents.isEmpty()) { { onChar(digit) } } else null,
                onAccentSelected = { onChar(it) }
            )
        }
    }

    // Row 2 Rendering
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (language == KeyboardLanguage.EN_US) {
            Spacer(modifier = Modifier.weight(0.5f))
        }
        row2.forEach { char ->
            val displayChar = if (isShifted) char.uppercase() else char
            val accents = accentsMap[char]?.map { if (isShifted) it.uppercase() else it } ?: emptyList()

            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = displayChar,
                accents = accents,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(displayChar) },
                onAccentSelected = { onChar(it) }
            )
        }
        if (language == KeyboardLanguage.EN_US) {
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }

    // Row 3 Rendering (Shift + Z..M + Backspace)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shift Key
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = Icons.Default.ArrowUpward,
            isAction = true,
            isAccent = shiftState != ShiftState.OFF,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onShiftToggle,
            onLongPress = onShiftToggle
        )

        row3.forEach { char ->
            val displayChar = if (isShifted) char.uppercase() else char
            val accents = accentsMap[char]?.map { if (isShifted) it.uppercase() else it } ?: emptyList()

            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = displayChar,
                accents = accents,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(displayChar) },
                onAccentSelected = { onChar(it) }
            )
        }

        // Backspace Key
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = Icons.AutoMirrored.Filled.Backspace,
            isAction = true,
            isRepeatable = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onBackspace
        )
    }

    // Row 4 Rendering (?123 + Emoji/Globe + Space + . + Enter)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numbers Switcher ?123
        KeyboardKey(
            modifier = Modifier.weight(1.4f),
            text = "?123",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onModeChange(KeyboardMode.NUMBERS) }
        )

        // Emoji / Globe Button
        if (showEmojiKey) {
            KeyboardKey(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEmotions,
                isAction = true,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onModeChange(KeyboardMode.EMOJI) }
            )
        } else {
            KeyboardKey(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Language,
                isAction = true,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = onLanguageToggle
            )
        }

        // Comma key
        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ",",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(",") }
        )

        // Space Bar (shows current language name)
        KeyboardKey(
            modifier = Modifier.weight(4.0f),
            text = if (language == KeyboardLanguage.PT_BR) "Português" else "English",
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onSpace,
            onLongPress = onLanguageToggle
        )

        // Period Key
        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ".",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(".") }
        )

        // Enter / Action Key
        val (enterIcon, isActionAccent) = getImeActionVisual(imeAction)
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = enterIcon,
            isAccent = isActionAccent,
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onEnter
        )
    }
}

@Composable
private fun NumbersLayout(
    showEmojiKey: Boolean,
    imeAction: Int,
    colors: KeyboardThemeColors,
    keyHeight: androidx.compose.ui.unit.Dp,
    hGap: androidx.compose.ui.unit.Dp,
    vGap: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    fontScale: Float,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onModeChange: (KeyboardMode) -> Unit
) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
    val row3 = listOf("!", "\"", "'", ":", ";", "/", "?")

    // Row 1
    Row(modifier = Modifier.fillMaxWidth()) {
        row1.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }
    }

    // Row 2
    Row(modifier = Modifier.fillMaxWidth()) {
        row2.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }
    }

    // Row 3
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch to Symbols =\<
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            text = "=\\<",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onModeChange(KeyboardMode.SYMBOLS) }
        )

        row3.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }

        // Backspace
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = Icons.AutoMirrored.Filled.Backspace,
            isAction = true,
            isRepeatable = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onBackspace
        )
    }

    // Row 4
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch to ABC
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            text = "ABC",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onModeChange(KeyboardMode.LETTERS) }
        )

        if (showEmojiKey) {
            KeyboardKey(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEmotions,
                isAction = true,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onModeChange(KeyboardMode.EMOJI) }
            )
        }

        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ",",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(",") }
        )

        KeyboardKey(
            modifier = Modifier.weight(4.0f),
            text = "espaço",
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onSpace
        )

        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ".",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(".") }
        )

        val (enterIcon, isActionAccent) = getImeActionVisual(imeAction)
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = enterIcon,
            isAccent = isActionAccent,
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onEnter
        )
    }
}

@Composable
private fun SymbolsLayout(
    showEmojiKey: Boolean,
    imeAction: Int,
    colors: KeyboardThemeColors,
    keyHeight: androidx.compose.ui.unit.Dp,
    hGap: androidx.compose.ui.unit.Dp,
    vGap: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    fontScale: Float,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onModeChange: (KeyboardMode) -> Unit
) {
    val row1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
    val row2 = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
    val row3 = listOf("%", "_", "[", "]", "<", ">", "¡", "¿")

    // Row 1
    Row(modifier = Modifier.fillMaxWidth()) {
        row1.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }
    }

    // Row 2
    Row(modifier = Modifier.fillMaxWidth()) {
        row2.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }
    }

    // Row 3
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch back to ?123
        KeyboardKey(
            modifier = Modifier.weight(1.4f),
            text = "?123",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onModeChange(KeyboardMode.NUMBERS) }
        )

        row3.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f),
                text = char,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onChar(char) }
            )
        }

        // Backspace
        KeyboardKey(
            modifier = Modifier.weight(1.4f),
            icon = Icons.AutoMirrored.Filled.Backspace,
            isAction = true,
            isRepeatable = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onBackspace
        )
    }

    // Row 4
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            text = "ABC",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onModeChange(KeyboardMode.LETTERS) }
        )

        if (showEmojiKey) {
            KeyboardKey(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEmotions,
                isAction = true,
                colors = colors,
                height = keyHeight,
                horizontalGap = hGap,
                verticalGap = vGap,
                cornerRadius = cornerRadius,
                fontScale = fontScale,
                onKeyPress = { onModeChange(KeyboardMode.EMOJI) }
            )
        }

        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ",",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(",") }
        )

        KeyboardKey(
            modifier = Modifier.weight(4.0f),
            text = "espaço",
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onSpace
        )

        KeyboardKey(
            modifier = Modifier.weight(0.9f),
            text = ".",
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = { onChar(".") }
        )

        val (enterIcon, isActionAccent) = getImeActionVisual(imeAction)
        KeyboardKey(
            modifier = Modifier.weight(1.5f),
            icon = enterIcon,
            isAccent = isActionAccent,
            isAction = true,
            colors = colors,
            height = keyHeight,
            horizontalGap = hGap,
            verticalGap = vGap,
            cornerRadius = cornerRadius,
            fontScale = fontScale,
            onKeyPress = onEnter
        )
    }
}

private fun getImeActionVisual(imeAction: Int): Pair<ImageVector, Boolean> {
    return when (imeAction and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_SEARCH -> Icons.Default.Search to true
        EditorInfo.IME_ACTION_SEND -> Icons.AutoMirrored.Filled.Send to true
        EditorInfo.IME_ACTION_DONE -> Icons.Default.Check to true
        EditorInfo.IME_ACTION_GO -> Icons.AutoMirrored.Filled.Send to true
        else -> Icons.Default.KeyboardReturn to false
    }
}
