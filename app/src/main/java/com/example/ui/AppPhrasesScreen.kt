package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.keyboard.data.AppPhrasesRepository
import com.example.keyboard.data.db.AppPhraseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPhrasesScreen(
    repository: AppPhrasesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        AppPhrasesRepository.CATEGORY_MESSAGING to "Mensagens & Chat",
        AppPhrasesRepository.CATEGORY_EMAIL to "E-mails & Formal",
        AppPhrasesRepository.CATEGORY_GENERAL to "Geral / Padrão"
    )

    val currentCategory = categories[selectedTabIndex].first

    val allPhrases by repository.allPhrasesFlow.collectAsStateWithLifecycle(emptyList())
    val currentPhrases = remember(allPhrases, currentCategory) {
        allPhrases.filter { it.appCategory == currentCategory }
    }

    var isAddingPhrase by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Frases Rápidas por App") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("phrases_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { repository.resetToDefaults() },
                        modifier = Modifier.testTag("phrases_reset_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restaurar padrões")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingPhrase = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_phrase_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Frase")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, (_, label) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = label,
                                maxLines = 1,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("phrase_tab_$index")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Info Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when (currentCategory) {
                                AppPhrasesRepository.CATEGORY_MESSAGING -> "Exibidas automaticamente no WhatsApp, Telegram, Instagram e apps de chat."
                                AppPhrasesRepository.CATEGORY_EMAIL -> "Exibidas automaticamente no Gmail, Outlook e apps de produtividade."
                                else -> "Exibidas como conjunto padrão nos demais aplicativos."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (currentPhrases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma frase nesta categoria.\nToque no botão + para adicionar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentPhrases, key = { it.id }) { phraseItem ->
                            PhraseItemCard(
                                phraseItem = phraseItem,
                                onDelete = { repository.deletePhrase(phraseItem.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Phrase Dialog
    if (isAddingPhrase) {
        var newPhraseText by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { isAddingPhrase = false },
            title = {
                Text("Adicionar Frase Rápida")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Categoria: ${categories[selectedTabIndex].second}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = newPhraseText,
                        onValueChange = {
                            newPhraseText = it
                            isError = false
                        },
                        label = { Text("Texto da frase") },
                        placeholder = { Text("ex: Segue comprovante em anexo.") },
                        maxLines = 3,
                        isError = isError && newPhraseText.isBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("phrase_text_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPhraseText.isBlank()) {
                            isError = true
                        } else {
                            repository.addPhrase(currentCategory, newPhraseText.trim())
                            isAddingPhrase = false
                        }
                    },
                    modifier = Modifier.testTag("phrase_save_btn")
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingPhrase = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PhraseItemCard(
    phraseItem: AppPhraseEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phrase_card_${phraseItem.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phraseItem.phrase,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp).testTag("delete_phrase_${phraseItem.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
