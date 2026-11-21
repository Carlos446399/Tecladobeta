package com.example.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.data.db.ClipboardItemEntity
import com.example.keyboard.model.KeyboardThemeColors

@Composable
fun ClipboardHistoryView(
    clips: List<ClipboardItemEntity>,
    colors: KeyboardThemeColors,
    onClipSelected: (String) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBackspace: () -> Unit,
    onSwitchToAlphabet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(colors.background)
    ) {
        // Clipboard Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(colors.actionKeyBackground)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = colors.accentKeyBackground,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Área de Transferência",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.keyTextColor
                )
                if (clips.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accentKeyBackground.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${clips.size}/20",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accentKeyBackground
                        )
                    }
                }
            }

            if (clips.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.keyBackground)
                        .clickable { onClearAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clipboard_clear_all_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpar tudo",
                            tint = colors.keySecondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Limpar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.keySecondaryTextColor
                        )
                    }
                }
            }
        }

        // Clips List or Empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            if (clips.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = colors.keySecondaryTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhum texto copiado recentemente",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.keyTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Textos copiados em outros apps aparecerão aqui para colar com 1 toque.",
                        fontSize = 11.sp,
                        color = colors.keySecondaryTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.keyBackground)
                                .border(1.dp, colors.dividerColor, RoundedCornerShape(8.dp))
                                .clickable { onClipSelected(clip.text) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("clip_item_${clip.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = clip.text,
                                        fontSize = 13.sp,
                                        color = colors.keyTextColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteClip(clip.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover",
                                        tint = colors.keySecondaryTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(colors.actionKeyBackground)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to ABC Keyboard
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keyBackground)
                    .clickable { onSwitchToAlphabet() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("clipboard_switch_abc"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "ABC",
                        tint = colors.keyTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ABC",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.keyTextColor
                    )
                }
            }

            // Backspace key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keyBackground)
                    .clickable { onBackspace() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("clipboard_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Apagar",
                    tint = colors.keyTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
