package com.example.keyboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.keyboard.model.KeyboardThemeColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun KeyboardKey(
    modifier: Modifier = Modifier,
    text: String = "",
    secondaryText: String? = null,
    icon: ImageVector? = null,
    colors: KeyboardThemeColors,
    isAction: Boolean = false,
    isAccent: Boolean = false,
    isRepeatable: Boolean = false,
    height: Dp = 46.dp,
    horizontalGap: Dp = 2.5.dp,
    verticalGap: Dp = 3.dp,
    cornerRadius: Dp = 8.dp,
    fontScale: Float = 1.0f,
    accents: List<String> = emptyList(),
    onKeyPress: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onAccentSelected: ((String) -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    var showAccentPopup by remember { mutableStateOf(false) }
    var selectedAccentIndex by remember { mutableStateOf<Int?>(null) }

    val currentOnKeyPress by rememberUpdatedState(onKeyPress)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnAccentSelected by rememberUpdatedState(onAccentSelected)
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        label = "keyScale"
    )

    val isBorderless = cornerRadius == 0.dp
    val isHexagonal = cornerRadius < 0.dp
    val keyShape = if (isHexagonal) KeyHexagonShape else RoundedCornerShape(cornerRadius)

    val keyBackground = when {
        isAccent -> colors.accentKeyBackground
        isAction -> if (isPressed) colors.keyBackgroundPressed else colors.actionKeyBackground
        isPressed -> colors.keyBackgroundPressed
        isBorderless -> colors.background
        else -> colors.keyBackground
    }

    val textColor = when {
        isAccent -> colors.accentTextColor
        else -> colors.keyTextColor
    }

    val elevation = if (isBorderless) 0.dp else if (isPressed) 0.5.dp else 1.5.dp

    Box(
        modifier = modifier
            .height(height)
            .padding(horizontal = horizontalGap, vertical = verticalGap),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .then(
                    if (elevation > 0.dp) {
                        Modifier.shadow(
                            elevation = elevation,
                            shape = keyShape,
                            clip = false
                        )
                    } else Modifier
                )
                .clip(keyShape)
                .background(keyBackground)
                .then(
                    when {
                        isHexagonal -> Modifier.border(
                            1.dp,
                            Brush.linearGradient(listOf(Color(0xFFFFD978), Color(0xFFB8791C), Color(0xFFFFE6A3))),
                            keyShape
                        )
                        colors.ledEffectEnabled && !isBorderless -> Modifier.border(
                            1.4.dp,
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF2D95),
                                    Color(0xFFFFC400), Color(0xFF00E676), Color(0xFF00E5FF)
                                )
                            ),
                            keyShape
                        )
                        colors.keyBorderColor != null && !isAccent && !isBorderless -> Modifier.border(
                            1.dp,
                            colors.keyBorderColor,
                            keyShape
                        )
                        else -> Modifier
                    }
                )
                .pointerInput(isRepeatable, accents, onLongPress != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isPressed = true
                        selectedAccentIndex = null

                        if (isRepeatable) {
                            // Instant execution on down
                            currentOnKeyPress()
                            var repeatJob: Job? = coroutineScope.launch {
                                delay(320)
                                var count = 0
                                while (isActive) {
                                    currentOnKeyPress()
                                    count++
                                    val interval = if (count > 15) 30L else if (count > 6) 45L else 65L
                                    delay(interval)
                                }
                            }
                            waitForUpOrCancellation()
                            repeatJob?.cancel()
                            repeatJob = null
                            isPressed = false
                        } else if (accents.isNotEmpty() || onLongPress != null) {
                            // Long-pressable key: check if held for 250ms
                            val upOrCancel = withTimeoutOrNull(250) {
                                waitForUpOrCancellation()
                            }
                            if (upOrCancel == null) {
                                // Timed out => Long press triggered!
                                if (accents.isNotEmpty()) {
                                    showAccentPopup = true
                                    val keyWidthPx = size.width.toFloat()
                                    val itemWidthPx = 44.dp.toPx()
                                    val totalPopupWidthPx = accents.size * itemWidthPx
                                    val popupLeftPx = (keyWidthPx - totalPopupWidthPx) / 2f

                                    // Track drag pointer movements until pointer goes up
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            change.consume()
                                            break
                                        }
                                        val currentX = change.position.x
                                        val relX = currentX - popupLeftPx
                                        val idx = (relX / itemWidthPx).toInt().coerceIn(0, accents.size - 1)
                                        selectedAccentIndex = idx
                                        change.consume()
                                    }

                                    // Pointer released: commit selected accent or first accent
                                    val finalIdx = selectedAccentIndex
                                    showAccentPopup = false
                                    selectedAccentIndex = null
                                    if (finalIdx != null && finalIdx in accents.indices) {
                                        currentOnAccentSelected?.invoke(accents[finalIdx]) ?: currentOnKeyPress()
                                    } else {
                                        // User long pressed but didn't drag to a specific accent: choose primary/first accent
                                        currentOnAccentSelected?.invoke(accents.first()) ?: currentOnKeyPress()
                                    }
                                } else {
                                    currentOnLongPress?.invoke()
                                    waitForUpOrCancellation()
                                }
                            } else {
                                // Released before timeout => Normal Tap
                                currentOnKeyPress()
                            }
                            isPressed = false
                        } else {
                            // Instant Tap key: fire immediately on pointer down for maximum responsiveness
                            currentOnKeyPress()
                            waitForUpOrCancellation()
                            isPressed = false
                        }
                    }
                }
                .testTag("key_${text.ifEmpty { icon?.name ?: "custom" }}"),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text.ifEmpty { "Key" },
                    tint = textColor,
                    modifier = Modifier.size((20 * fontScale).dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (secondaryText != null) {
                        Text(
                            text = secondaryText,
                            fontSize = (9 * fontScale).sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.keySecondaryTextColor,
                            lineHeight = 10.sp,
                            modifier = Modifier.offset(y = (-1).dp)
                        )
                    }
                    Text(
                        text = text,
                        fontSize = if (text.length > 2) (13 * fontScale).sp else (19 * fontScale).sp,
                        fontWeight = if (isAction || isAccent || text.length > 1) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Accents Popup on Long Press
        if (showAccentPopup && accents.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -140),
                onDismissRequest = {
                    showAccentPopup = false
                    selectedAccentIndex = null
                }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.actionKeyBackground),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        accents.forEachIndexed { index, accentChar ->
                            val isHighlighted = selectedAccentIndex == index
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isHighlighted) colors.accentKeyBackground else colors.keyBackground)
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.border(1.5.dp, colors.accentTextColor, RoundedCornerShape(8.dp))
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = accentChar,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighlighted) colors.accentTextColor else colors.keyTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
