package com.example.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.reader.FloatingReaderPreferences
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardThemeColors

@Composable
fun SuggestionBar(
    suggestions: List<String>,
    quickPhrases: List<String> = emptyList(),
    quickPhraseCategoryBadge: String = "Geral",
    isQuickPhrasesMode: Boolean = false,
    inferredToneLabel: String? = null,
    isVoiceRecording: Boolean = false,
    currentLanguage: KeyboardLanguage,
    colors: KeyboardThemeColors,
    isSecureFieldActive: Boolean = false,
    isIncognitoActive: Boolean = false,
    onSuggestionClick: (String) -> Unit,
    onQuickPhraseClick: (String) -> Unit = {},
    onToggleQuickPhrases: () -> Unit = {},
    onToggleIncognito: () -> Unit = {},
    onToggleVoiceInput: () -> Unit = {},
    onLanguageToggle: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenTranslate: () -> Unit = {},
    onOpenOcr: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onToggleFloatingReader: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phrasesScrollState = rememberScrollState()
    val toolsScrollState = rememberScrollState()
    var isShieldToolsOpen by remember { mutableStateOf(false) }
    var isFloatingReaderEnabled by remember(context) {
        mutableStateOf(FloatingReaderPreferences.isEnabled(context))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isVoiceRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.suggestionBackground)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isShieldToolsOpen,
            modifier = Modifier.fillMaxWidth().padding(end = 38.dp),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "shieldDrawerTransition"
        ) { toolsOpen ->
            if (toolsOpen) {
                // SHIELD TOOLS DRAWER ("Guarda os outros ícones")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(toolsScrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // The shield itself is anchored at the right edge; this label stays with the drawer.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentKeyBackground.copy(alpha = 0.2f))
                            .border(1.dp, colors.accentKeyBackground, RoundedCornerShape(8.dp))
                            .clickable { isShieldToolsOpen = false }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("shield_drawer_header"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ferramentas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentKeyBackground
                            )
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 8.dp)
                            .width(1.dp),
                        color = colors.dividerColor
                    )

                    // 1. Voice Mic Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onToggleVoiceInput()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_voice")
                    ) {
                        Icon(
                            imageVector = if (isVoiceRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Digitação por Voz",
                            tint = if (isVoiceRecording) colors.accentKeyBackground else colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Incognito Privacy Tool
                    IconButton(
                        onClick = {
                            onToggleIncognito()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_incognito")
                    ) {
                        Icon(
                            imageVector = if (isIncognitoActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isIncognitoActive) "Desativar Incógnito" else "Ativar Incógnito",
                            tint = if (isIncognitoActive) colors.accentKeyBackground else colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Quick Phrases Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onToggleQuickPhrases()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_quick_phrases")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quickreply,
                            contentDescription = "Frases Rápidas",
                            tint = colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Translate Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onOpenTranslate()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_translate")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Tradução Offline",
                            tint = colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 5. OCR Scanner Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onOpenOcr()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_ocr")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scanner OCR",
                            tint = colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 6. Clipboard Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onOpenClipboard()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_clipboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Área de Transferência",
                            tint = colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Floating reader visibility toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFloatingReaderEnabled) colors.accentKeyBackground.copy(alpha = 0.2f) else colors.actionKeyBackground)
                            .clickable {
                            val nextEnabled = !isFloatingReaderEnabled
                            isFloatingReaderEnabled = nextEnabled
                            onToggleFloatingReader(nextEnabled)
                            }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("shield_tool_floating_reader"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFloatingReaderEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isFloatingReaderEnabled) colors.accentKeyBackground else colors.keyTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isFloatingReaderEnabled) "Bolha ON" else "Bolha OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.keyTextColor
                            )
                        }
                    }

                    // 7. Settings Tool
                    IconButton(
                        onClick = {
                            isShieldToolsOpen = false
                            onOpenSettings()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = colors.keyTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 8. Close Button
                    IconButton(
                        onClick = { isShieldToolsOpen = false },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("shield_tool_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Ferramentas",
                            tint = colors.keySecondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // NORMAL SUGGESTIONS & PROMINENT SHIELD / VOICE BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Badge indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.actionKeyBackground)
                            .clickable { onLanguageToggle() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("suggestion_lang_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentLanguage.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentKeyBackground
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .width(1.dp),
                        color = colors.dividerColor
                    )

                    // Center area: Voice Recording Indicator OR Secure/Incognito OR Quick Phrases OR Suggestions
                    if (isVoiceRecording) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.accentKeyBackground.copy(alpha = 0.25f))
                                    .border(1.dp, colors.accentKeyBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Ouvindo...",
                                        tint = colors.accentKeyBackground,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .scale(pulseScale)
                                    )
                                    Text(
                                        text = "Ouvindo voz...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.keyTextColor
                                    )
                                }
                            }
                        }
                    } else if (isSecureFieldActive || isIncognitoActive) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isIncognitoActive) colors.accentKeyBackground.copy(alpha = 0.2f)
                                        else colors.actionKeyBackground.copy(alpha = 0.6f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isIncognitoActive) Icons.Default.VisibilityOff else Icons.Default.Lock,
                                        contentDescription = if (isIncognitoActive) "Modo Incógnito Ativo" else "Modo Seguro",
                                        tint = colors.accentKeyBackground,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isIncognitoActive) "Modo Incógnito Ativo" else "Modo seguro ativo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.keyTextColor
                                    )
                                }
                            }
                        }
                    } else if (isQuickPhrasesMode) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .horizontalScroll(phrasesScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accentKeyBackground.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚡ $quickPhraseCategoryBadge",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentKeyBackground
                                )
                            }

                            if (inferredToneLabel != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.actionKeyBackground)
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                    Text(
                                        text = inferredToneLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.keySecondaryTextColor
                                    )
                                }
                            }

                            quickPhrases.forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBackground)
                                        .border(0.5.dp, colors.dividerColor, RoundedCornerShape(8.dp))
                                        .clickable { onQuickPhraseClick(phrase) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("quick_phrase_chip"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = phrase,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.keyTextColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        // Spacious Suggestions List
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (suggestions.isEmpty()) {
                                Text(
                                    text = "…",
                                    color = colors.keySecondaryTextColor,
                                    fontSize = 14.sp
                                )
                            } else {
                                suggestions.take(3).forEachIndexed { index, suggestion ->
                                    val isCenterPrimary = index == (suggestions.size / 2).coerceAtMost(1)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp, vertical = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isCenterPrimary && suggestions.size > 1)
                                                    colors.keyBackground
                                                else
                                                    androidx.compose.ui.graphics.Color.Transparent
                                            )
                                            .clickable { onSuggestionClick(suggestion) }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = if (isCenterPrimary) 15.sp else 14.sp,
                                            fontWeight = if (isCenterPrimary) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCenterPrimary) colors.keyTextColor else colors.keySecondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    if (index < suggestions.take(3).size - 1) {
                                        VerticalDivider(
                                            modifier = Modifier
                                                .padding(vertical = 10.dp)
                                                .width(1.dp),
                                            color = colors.dividerColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .width(1.dp),
                        color = colors.dividerColor
                    )

                    // Microphone Voice Input Button
                    IconButton(
                        onClick = onToggleVoiceInput,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("suggestion_voice_btn")
                    ) {
                        Icon(
                            imageVector = if (isVoiceRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Digitação por Voz",
                            tint = if (isVoiceRecording) colors.accentKeyBackground else colors.keySecondaryTextColor,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(if (isVoiceRecording) pulseScale else 1.0f)
                        )
                    }

                    IconButton(
                        onClick = onOpenTranslate,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("suggestion_translate_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Abrir tradução",
                            tint = colors.keySecondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                }
            }
        }

        // Fixed shield: tool icons slide in/out to its left without moving this control.
        IconButton(
            onClick = { isShieldToolsOpen = !isShieldToolsOpen },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(34.dp)
                .testTag("suggestion_shield_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = if (isShieldToolsOpen) "Recolher Ferramentas" else "Abrir Ferramentas",
                tint = colors.accentKeyBackground,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
