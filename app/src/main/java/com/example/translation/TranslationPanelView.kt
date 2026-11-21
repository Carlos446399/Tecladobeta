package com.example.translation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.keyboard.model.KeyboardThemeColors
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.launch

@Composable
fun TranslationPanelView(
    translationManager: TranslationManager,
    colors: KeyboardThemeColors,
    initialText: String = "",
    onInsertText: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val status by translationManager.status.collectAsStateWithLifecycle()

    var sourceLang by remember { mutableStateOf(TranslateLanguage.PORTUGUESE) }
    var targetLang by remember { mutableStateOf(TranslateLanguage.ENGLISH) }

    var inputText by remember { mutableStateOf(initialText) }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var targetMenuExpanded by remember { mutableStateOf(false) }

    val areBothModelsReady = status.isSourceDownloaded && status.isTargetDownloaded

    // Check model status whenever source/target change
    LaunchedEffect(sourceLang, targetLang) {
        translationManager.checkModelsDownloaded(sourceLang, targetLang)
    }

    // Auto-translate on input change or language change if models are ready
    LaunchedEffect(inputText, sourceLang, targetLang, areBothModelsReady) {
        if (inputText.isNotBlank() && areBothModelsReady) {
            isTranslating = true
            val res = translationManager.translate(inputText, sourceLang, targetLang)
            isTranslating = false
            res.onSuccess { translatedText = it }
                .onFailure { translatedText = "" }
        } else if (inputText.isBlank()) {
            translatedText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Top Toolbar: Languages & Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Source Language Selector
                Box {
                    val sourceObj = translationManager.supportedLanguages.find { it.code == sourceLang }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.actionKeyBackground)
                            .clickable { sourceMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("trans_source_lang_btn")
                    ) {
                        Text(
                            text = "${sourceObj?.flag ?: ""} ${sourceObj?.displayName ?: sourceLang}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.keyTextColor
                        )
                    }
                    DropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        translationManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.flag} ${lang.displayName}") },
                                onClick = {
                                    sourceLang = lang.code
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Swap Languages Button
                IconButton(
                    onClick = {
                        val temp = sourceLang
                        sourceLang = targetLang
                        targetLang = temp
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.actionKeyBackground)
                        .testTag("trans_swap_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Inverter Idiomas",
                        tint = colors.accentKeyBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Target Language Selector
                Box {
                    val targetObj = translationManager.supportedLanguages.find { it.code == targetLang }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.actionKeyBackground)
                            .clickable { targetMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("trans_target_lang_btn")
                    ) {
                        Text(
                            text = "${targetObj?.flag ?: ""} ${targetObj?.displayName ?: targetLang}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.keyTextColor
                        )
                    }
                    DropdownMenu(
                        expanded = targetMenuExpanded,
                        onDismissRequest = { targetMenuExpanded = false }
                    ) {
                        translationManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.flag} ${lang.displayName}") },
                                onClick = {
                                    targetLang = lang.code
                                    targetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Close / Switch back to normal keyboard
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.actionKeyBackground)
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("trans_close_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Teclado",
                        tint = colors.keyTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ABC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.keyTextColor
                    )
                }
            }
        }

        // Offline / Online Download Banner
        if (!areBothModelsReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentKeyBackground.copy(alpha = 0.15f))
                    .border(1.dp, colors.accentKeyBackground.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download de modelo necessário",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.keyTextColor
                        )
                        Text(
                            text = if (status.isDownloading) status.downloadProgressText else "Requer internet apenas na 1ª vez (depois 100% offline).",
                            fontSize = 10.sp,
                            color = colors.keySecondaryTextColor
                        )
                    }

                    if (status.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.accentKeyBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = {
                                translationManager.downloadModels(sourceLang, targetLang)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentKeyBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            modifier = Modifier.height(30.dp).testTag("trans_download_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Baixar", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Main translation workspace
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Source Text Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.keyBackground)
                    .border(0.5.dp, colors.dividerColor, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Original",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.keySecondaryTextColor
                        )
                        if (inputText.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar",
                                tint = colors.keySecondaryTextColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        inputText = ""
                                        translatedText = ""
                                    }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Digite para traduzir...", fontSize = 12.sp, color = colors.keySecondaryTextColor) },
                        modifier = Modifier.fillMaxSize().testTag("trans_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.keyTextColor,
                            unfocusedTextColor = colors.keyTextColor
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                }
            }

            // Target Translated Output Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.actionKeyBackground)
                    .border(0.5.dp, colors.dividerColor, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (areBothModelsReady) "⚡ Tradução Offline" else "Tradução",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (areBothModelsReady) Color(0xFF10B981) else colors.keySecondaryTextColor
                        )
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = colors.accentKeyBackground
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        if (translatedText.isNotEmpty()) {
                            Text(
                                text = translatedText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.keyTextColor,
                                modifier = Modifier.verticalScroll(rememberScrollState()).testTag("trans_output_text")
                            )
                        } else {
                            Text(
                                text = if (!areBothModelsReady) "Baixe os modelos acima para traduzir offline" else "A tradução aparecerá aqui...",
                                fontSize = 11.sp,
                                color = colors.keySecondaryTextColor
                            )
                        }
                    }

                    if (translatedText.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(translatedText))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = colors.keySecondaryTextColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action: Insert into Field
        if (translatedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    onInsertText(translatedText)
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("trans_insert_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Inserir Tradução no Campo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
