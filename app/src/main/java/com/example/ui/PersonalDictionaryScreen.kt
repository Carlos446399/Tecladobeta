package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.keyboard.data.UserDictionaryRepository
import com.example.keyboard.data.db.UserDictionaryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDictionaryScreen(
    repository: UserDictionaryRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val words by repository.allWordsFlow.collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilter by remember { mutableStateOf("ALL") } // "ALL", "pt_br", "en_us"
    var wordToEdit by remember { mutableStateOf<UserDictionaryEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var wordToDelete by remember { mutableStateOf<UserDictionaryEntity?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val filteredWords = remember(words, searchQuery, selectedLanguageFilter) {
        words.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.word.contains(searchQuery, ignoreCase = true) ||
                    item.shortcut.contains(searchQuery, ignoreCase = true)
            }

            val matchesLang = when (selectedLanguageFilter) {
                "ALL" -> true
                "pt_br" -> item.language.equals("pt_br", ignoreCase = true) || item.language.equals("ALL", ignoreCase = true)
                "en_us" -> item.language.equals("en_us", ignoreCase = true) || item.language.equals("ALL", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesLang
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.personal_dict_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${words.size} palavras aprendidas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dict_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("dict_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restaurar padrões"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_word_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Palavra"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Explanation Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("dict_info_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spellcheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Palavras Salvas no Teclado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Palavras no dicionário nunca são marcadas como erro, evitam autocorreções indevidas e aparecem com prioridade na barra de sugestões.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_dictionary_input"),
                placeholder = { Text("Pesquisar palavra no dicionário...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Pesquisar",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar pesquisa",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Language Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedLanguageFilter == "ALL",
                        onClick = { selectedLanguageFilter = "ALL" },
                        label = { Text("Todos (${words.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedLanguageFilter == "pt_br",
                        onClick = { selectedLanguageFilter = "pt_br" },
                        label = { Text("Português 🇧🇷", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("filter_pt")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedLanguageFilter == "en_us",
                        onClick = { selectedLanguageFilter = "en_us" },
                        label = { Text("Inglês 🇺🇸", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("filter_en")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Word List
            if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhuma palavra cadastrada" else "Nenhum resultado para \"$searchQuery\"",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Toque no botão + abaixo para adicionar uma nova palavra.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredWords,
                        key = { it.id }
                    ) { wordEntity ->
                        WordItemCard(
                            entity = wordEntity,
                            onEdit = { wordToEdit = wordEntity },
                            onDelete = { wordToDelete = wordEntity }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddingNew || wordToEdit != null) {
        AddEditWordDialog(
            initialEntity = wordToEdit,
            onDismiss = {
                isAddingNew = false
                wordToEdit = null
            },
            onSave = { word, language, shortcut ->
                if (wordToEdit != null) {
                    repository.updateWord(
                        wordToEdit!!.copy(
                            word = word,
                            language = language,
                            shortcut = shortcut
                        )
                    )
                } else {
                    repository.addWord(word = word, language = language, shortcut = shortcut)
                }
                isAddingNew = false
                wordToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (wordToDelete != null) {
        AlertDialog(
            onDismissRequest = { wordToDelete = null },
            title = { Text("Excluir Palavra") },
            text = { Text("Deseja remover \"${wordToDelete?.word}\" do seu dicionário pessoal?") },
            confirmButton = {
                Button(
                    onClick = {
                        wordToDelete?.let { repository.deleteWord(it) }
                        wordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { wordToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Restaurar Dicionário") },
            text = { Text("Deseja restaurar as palavras padrão do dicionário pessoal? Suas palavras customizadas serão reiniciadas.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetToDefaults()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WordItemCard(
    entity: UserDictionaryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("word_item_${entity.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entity.word,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val (langLabel, containerColor, textColor) = when (entity.language.lowercase()) {
                        "pt_br" -> Triple("PT-BR", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        "en_us" -> Triple("EN-US", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        else -> Triple("TODOS", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(containerColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = langLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }

                if (entity.shortcut.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Atalho: ${entity.shortcut}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("edit_word_${entity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_word_${entity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditWordDialog(
    initialEntity: UserDictionaryEntity?,
    onDismiss: () -> Unit,
    onSave: (word: String, language: String, shortcut: String) -> Unit
) {
    var word by remember { mutableStateOf(initialEntity?.word ?: "") }
    var shortcut by remember { mutableStateOf(initialEntity?.shortcut ?: "") }
    var language by remember { mutableStateOf(initialEntity?.language ?: "ALL") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEntity == null) "Adicionar Palavra" else "Editar Palavra",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = {
                        word = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Palavra / Termo") },
                    placeholder = { Text("Ex: Kubernetes, Microserviço...") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth().testTag("word_input_field")
                )

                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Atalho Opcional") },
                    placeholder = { Text("Ex: k8s") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("word_shortcut_field")
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Idioma:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = language.equals("ALL", ignoreCase = true),
                            onClick = { language = "ALL" },
                            label = { Text("Todos", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = language.equals("pt_br", ignoreCase = true),
                            onClick = { language = "pt_br" },
                            label = { Text("PT-BR", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = language.equals("en_us", ignoreCase = true),
                            onClick = { language = "en_us" },
                            label = { Text("EN-US", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (word.trim().isBlank()) {
                        errorMessage = "Digite a palavra"
                    } else {
                        onSave(word.trim(), language, shortcut.trim())
                    }
                },
                modifier = Modifier.testTag("save_word_btn")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
