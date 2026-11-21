package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.keyboard.data.TextShortcutRepository
import com.example.keyboard.data.db.TextShortcutEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextShortcutsScreen(
    repository: TextShortcutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts by repository.allShortcutsFlow.collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var shortcutToEdit by remember { mutableStateOf<TextShortcutEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val filteredShortcuts = remember(shortcuts, searchQuery) {
        if (searchQuery.isBlank()) {
            shortcuts
        } else {
            shortcuts.filter {
                it.trigger.contains(searchQuery, ignoreCase = true) ||
                    it.expansion.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Atalhos de Texto (Expansão)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("shortcuts_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { repository.resetToDefaults() },
                        modifier = Modifier.testTag("shortcuts_reset_btn")
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
                onClick = { isAddingNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_shortcut_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Atalho")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header explanation card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Digite o gatilho e aperte espaço ou pontuação para expandir automaticamente.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar atalhos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("shortcuts_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Shortcuts List
            if (filteredShortcuts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Nenhum atalho cadastrado.\nToque no botão + para criar." else "Nenhum atalho encontrado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredShortcuts, key = { it.id }) { shortcut ->
                        ShortcutItemCard(
                            shortcut = shortcut,
                            onToggle = { repository.toggleShortcut(shortcut) },
                            onEdit = { shortcutToEdit = shortcut },
                            onDelete = { repository.deleteShortcut(shortcut.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddingNew || shortcutToEdit != null) {
        val isEditing = shortcutToEdit != null
        var triggerText by remember(shortcutToEdit) { mutableStateOf(shortcutToEdit?.trigger ?: "") }
        var expansionText by remember(shortcutToEdit) { mutableStateOf(shortcutToEdit?.expansion ?: "") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                isAddingNew = false
                shortcutToEdit = null
            },
            title = {
                Text(if (isEditing) "Editar Atalho" else "Novo Atalho de Texto")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = triggerText,
                        onValueChange = {
                            triggerText = it
                            isError = false
                        },
                        label = { Text("Gatilho (ex: vlw)") },
                        placeholder = { Text("ex: vlw, obg, msg") },
                        singleLine = true,
                        isError = isError && triggerText.isBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("shortcut_trigger_input")
                    )

                    OutlinedTextField(
                        value = expansionText,
                        onValueChange = {
                            expansionText = it
                            isError = false
                        },
                        label = { Text("Texto Expandido") },
                        placeholder = { Text("ex: Valeu, muito obrigado!") },
                        maxLines = 3,
                        isError = isError && expansionText.isBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("shortcut_expansion_input")
                    )

                    if (isError) {
                        Text(
                            text = "Preencha tanto o gatilho quanto a expansão.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (triggerText.isBlank() || expansionText.isBlank()) {
                            isError = true
                        } else {
                            if (isEditing && shortcutToEdit != null) {
                                repository.updateShortcut(
                                    shortcutToEdit!!.copy(
                                        trigger = triggerText.trim(),
                                        expansion = expansionText.trim()
                                    )
                                )
                            } else {
                                repository.addShortcut(triggerText.trim(), expansionText.trim())
                            }
                            isAddingNew = false
                            shortcutToEdit = null
                        }
                    },
                    modifier = Modifier.testTag("shortcut_save_btn")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddingNew = false
                    shortcutToEdit = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ShortcutItemCard(
    shortcut: TextShortcutEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shortcut_card_${shortcut.trigger}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trigger Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shortcut.trigger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Expansion preview
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.expansion,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (shortcut.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Edit button
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Switch toggle
            Switch(
                checked = shortcut.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(start = 4.dp).testTag("shortcut_switch_${shortcut.trigger}")
            )
        }
    }
}
