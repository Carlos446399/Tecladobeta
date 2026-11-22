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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.data.EmojiData
import com.example.keyboard.model.KeyboardThemeColors
import com.example.stickers.GiphyMediaItem
import com.example.stickers.StickerData
import com.example.stickers.StickerItem

enum class MediaTab {
    EMOJI,
    STICKERS,
    GIFS
}

@Composable
fun EmojiPickerView(
    colors: KeyboardThemeColors,
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (StickerItem) -> Unit = {},
    onGifSelected: (GiphyMediaItem) -> Unit = {},
    allowOnlineSearch: Boolean = true,
    onBackspace: () -> Unit,
    onSwitchToAlphabet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(MediaTab.EMOJI) }
    var selectedEmojiCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedStickerCategoryIndex by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val emojiCategories = remember(context) { EmojiData.load(context) }
    val currentEmojiCategory = emojiCategories.getOrElse(selectedEmojiCategoryIndex) { emojiCategories.first() }
    val currentStickerCategory = StickerData.categories[selectedStickerCategoryIndex]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(275.dp)
            .background(colors.background)
    ) {
        // Top Master Tab Row: Emojis | Figurinhas | GIFs (em breve)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(colors.actionKeyBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Emojis Tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == MediaTab.EMOJI) colors.keyBackground else Color.Transparent)
                    .clickable { activeTab = MediaTab.EMOJI }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("tab_emojis"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("😀", fontSize = 14.sp)
                    Text(
                        text = "Emojis",
                        fontSize = 12.sp,
                        fontWeight = if (activeTab == MediaTab.EMOJI) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == MediaTab.EMOJI) colors.accentKeyBackground else colors.keyTextColor
                    )
                }
            }

            // Figurinhas Tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == MediaTab.STICKERS) colors.keyBackground else Color.Transparent)
                    .clickable { activeTab = MediaTab.STICKERS }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("tab_stickers"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🖼️", fontSize = 14.sp)
                    Text(
                        text = "Figurinhas",
                        fontSize = 12.sp,
                        fontWeight = if (activeTab == MediaTab.STICKERS) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == MediaTab.STICKERS) colors.accentKeyBackground else colors.keyTextColor
                    )
                }
            }

            // GIF search is available through an explicitly configured provider.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == MediaTab.GIFS) colors.keyBackground else Color.Transparent)
                    .clickable { activeTab = MediaTab.GIFS }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("tab_gifs"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("GIF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accentKeyBackground)
                    Text(
                        text = "GIFs",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == MediaTab.GIFS) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == MediaTab.GIFS) colors.accentKeyBackground else colors.keyTextColor
                    )
                }
            }
        }

        // Sub-content based on active Tab
        when (activeTab) {
            MediaTab.EMOJI -> {
                // Emoji Sub-Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedEmojiCategoryIndex,
                    containerColor = colors.background,
                    contentColor = colors.keyTextColor,
                    edgePadding = 6.dp,
                    divider = {},
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    emojiCategories.forEachIndexed { index, category ->
                        val isSelected = selectedEmojiCategoryIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedEmojiCategoryIndex = index },
                            modifier = Modifier.padding(horizontal = 2.dp),
                            text = {
                                Text(
                                    text = category.icon,
                                    fontSize = if (isSelected) 18.sp else 15.sp
                                )
                            }
                        )
                    }
                }

                // Emoji Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 42.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(currentEmojiCategory.emojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEmojiSelected(emoji) }
                                    .testTag("emoji_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            MediaTab.STICKERS -> {
                // Sticker Categories Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedStickerCategoryIndex,
                    containerColor = colors.background,
                    contentColor = colors.keyTextColor,
                    edgePadding = 6.dp,
                    divider = {},
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    StickerData.categories.forEachIndexed { index, catName ->
                        val isSelected = selectedStickerCategoryIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedStickerCategoryIndex = index },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            text = {
                                Text(
                                    text = catName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.accentKeyBackground else colors.keySecondaryTextColor
                                )
                            }
                        )
                    }
                }

                // Sticker Grid (Static, 100% offline)
                val filteredStickers = remember(selectedStickerCategoryIndex) {
                    StickerData.stickers.filter { it.category == currentStickerCategory }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredStickers) { sticker ->
                            Box(
                                modifier = Modifier
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(sticker.bgColorHex))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable { onStickerSelected(sticker) }
                                    .padding(6.dp)
                                    .testTag("sticker_${sticker.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = sticker.emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sticker.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MediaTab.GIFS -> {
                GiphyMediaPanel(
                    colors = colors,
                    onMediaSelected = onGifSelected,
                    allowOnlineSearch = allowOnlineSearch,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom control bar (ABC switch & backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
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
                    .testTag("emoji_switch_abc"),
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
                    .testTag("emoji_backspace"),
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
