package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.data.db.KeyboardDatabase
import com.example.keyboard.data.db.TextShortcutDao
import com.example.keyboard.data.db.TextShortcutEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TextShortcutRepository private constructor(context: Context) {

    private val db = KeyboardDatabase.getInstance(context)
    private val dao: TextShortcutDao = db.textShortcutDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    val allShortcutsFlow: Flow<List<TextShortcutEntity>> = dao.getAllShortcutsFlow()

    // In-memory lookup map for zero-latency expansion during typing
    private val _enabledMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val enabledMap = _enabledMap.asStateFlow()

    init {
        scope.launch {
            if (dao.getCount() == 0) {
                dao.insertAll(DEFAULT_SHORTCUTS)
            }
            dao.getAllShortcutsFlow().collect { list ->
                val map = list.filter { it.isEnabled }.associate { it.trigger.lowercase() to it.expansion }
                _enabledMap.value = map
            }
        }
    }

    fun getExpansion(trigger: String): String? {
        val clean = trigger.trim().lowercase()
        return _enabledMap.value[clean]
    }

    fun addShortcut(trigger: String, expansion: String) {
        val cleanTrigger = trigger.trim()
        val cleanExp = expansion.trim()
        if (cleanTrigger.isEmpty() || cleanExp.isEmpty()) return

        scope.launch {
            dao.insert(
                TextShortcutEntity(
                    trigger = cleanTrigger,
                    expansion = cleanExp,
                    isEnabled = true
                )
            )
        }
    }

    fun updateShortcut(shortcut: TextShortcutEntity) {
        scope.launch {
            dao.update(shortcut)
        }
    }

    fun toggleShortcut(shortcut: TextShortcutEntity) {
        scope.launch {
            dao.update(shortcut.copy(isEnabled = !shortcut.isEnabled))
        }
    }

    fun deleteShortcut(id: Long) {
        scope.launch {
            dao.deleteById(id)
        }
    }

    fun resetToDefaults() {
        scope.launch {
            dao.clearAll()
            dao.insertAll(DEFAULT_SHORTCUTS)
        }
    }

    companion object {
        val DEFAULT_SHORTCUTS = listOf(
            TextShortcutEntity(trigger = "vlw", expansion = "Valeu, muito obrigado!"),
            TextShortcutEntity(trigger = "obg", expansion = "Obrigado!"),
            TextShortcutEntity(trigger = "vc", expansion = "você"),
            TextShortcutEntity(trigger = "pq", expansion = "porque"),
            TextShortcutEntity(trigger = "tbm", expansion = "também"),
            TextShortcutEntity(trigger = "abs", expansion = "Um grande abraço!"),
            TextShortcutEntity(trigger = "att", expansion = "Atenciosamente,")
        )

        @Volatile
        private var instance: TextShortcutRepository? = null

        fun getInstance(context: Context): TextShortcutRepository {
            return instance ?: synchronized(this) {
                instance ?: TextShortcutRepository(context).also { instance = it }
            }
        }
    }
}
