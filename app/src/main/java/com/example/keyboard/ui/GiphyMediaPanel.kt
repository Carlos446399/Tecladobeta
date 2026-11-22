package com.example.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.keyboard.model.KeyboardThemeColors
import com.example.stickers.GiphyMediaItem
import com.example.stickers.GiphyMediaRepository
import kotlinx.coroutines.launch

@Composable
fun GiphyMediaPanel(
    colors: KeyboardThemeColors,
    onMediaSelected: (GiphyMediaItem) -> Unit,
    allowOnlineSearch: Boolean,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var isSticker by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GiphyMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasRequested by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(allowOnlineSearch) {
        if (!allowOnlineSearch) results = emptyList()
    }

    fun load(trending: Boolean) {
        if (!allowOnlineSearch) {
            error = "A busca online fica desativada em campos seguros e no modo anônimo."
            results = emptyList()
            return
        }
        if (!GiphyMediaRepository.isConfigured()) {
            error = "Integração não configurada. Adicione sua chave GIPHY_API_KEY ao local.properties e gere o APK novamente."
            results = emptyList()
            return
        }
        isLoading = true
        error = null
        hasRequested = true
        scope.launch {
            try {
                results = if (trending) {
                    GiphyMediaRepository.trending(isSticker)
                } else {
                    GiphyMediaRepository.search(query, isSticker)
                }
            } catch (exception: Exception) {
                error = exception.message ?: "Não foi possível carregar o conteúdo agora."
                results = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(50) },
                modifier = Modifier.weight(1f).height(52.dp).testTag("giphy_query"),
                enabled = allowOnlineSearch,
                singleLine = true,
                placeholder = { Text("Buscar GIF ou figurinha", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { load(trending = false) })
            )
            IconButton(
                onClick = { load(trending = false) },
                enabled = allowOnlineSearch && query.isNotBlank() && !isLoading,
                modifier = Modifier.testTag("giphy_search")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Buscar no GIPHY", tint = colors.accentKeyBackground)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = !isSticker,
                onClick = { isSticker = false; results = emptyList(); hasRequested = false; error = null },
                label = { Text("GIFs", fontSize = 11.sp) },
                enabled = allowOnlineSearch,
                modifier = Modifier.testTag("giphy_type_gifs")
            )
            FilterChip(
                selected = isSticker,
                onClick = { isSticker = true; results = emptyList(); hasRequested = false; error = null },
                label = { Text("Figurinhas", fontSize = 11.sp) },
                enabled = allowOnlineSearch,
                modifier = Modifier.testTag("giphy_type_stickers")
            )
            IconButton(
                onClick = { load(trending = true) },
                enabled = allowOnlineSearch && !isLoading,
                modifier = Modifier.testTag("giphy_trending")
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = "Em alta", tint = colors.accentKeyBackground)
            }
            Text("Em alta", fontSize = 10.sp, color = colors.keySecondaryTextColor)
        }

        Text(
            text = "Powered by GIPHY · A busca só é enviada ao tocar em Buscar ou Em alta.",
            fontSize = 9.sp,
            color = colors.keySecondaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        when {
            isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accentKeyBackground)
            }
            !error.isNullOrBlank() -> Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text(error.orEmpty(), color = colors.keySecondaryTextColor, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
            results.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (!allowOnlineSearch) {
                        "Busca externa desativada em campos seguros e no modo anônimo."
                    } else if (!GiphyMediaRepository.isConfigured()) {
                        "Para pesquisar conteúdo atual, configure a chave GIPHY_API_KEY no arquivo local.properties. Nenhum texto será enviado sem uma ação sua."
                    } else if (hasRequested) {
                        "Nenhum resultado. Tente outro termo."
                    } else {
                        "Digite um termo e toque em Buscar, ou toque em Em alta para ver tendências."
                    },
                    color = colors.keySecondaryTextColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 76.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("giphy_results"),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .height(76.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.actionKeyBackground)
                            .clickable { onMediaSelected(item) }
                            .testTag("giphy_item_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.previewUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (item.isSticker) {
                            Text(
                                text = item.title,
                                color = colors.keyTextColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .background(colors.background.copy(alpha = 0.72f)).padding(2.dp),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
