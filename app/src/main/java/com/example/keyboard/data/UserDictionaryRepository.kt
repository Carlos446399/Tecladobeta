package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.data.db.KeyboardDatabase
import com.example.keyboard.data.db.UserDictionaryDao
import com.example.keyboard.data.db.UserDictionaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class UserDictionaryRepository private constructor(
    private val dao: UserDictionaryDao
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allWordsFlow: Flow<List<UserDictionaryEntity>> = dao.getAllWordsFlow()

    init {
        // Preload default words on initial setup if table is empty
        repositoryScope.launch {
            if (dao.getCount() == 0) {
                dao.insertAll(DEFAULT_WORDS)
            }
            // Keep WordDictionary synchronized
            allWordsFlow.distinctUntilChanged().collect { list ->
                WordDictionary.setUserCustomWords(list.map { it.word })
            }
        }
    }

    fun getWordsByLanguageFlow(language: String): Flow<List<UserDictionaryEntity>> {
        return if (language.equals("ALL", ignoreCase = true)) {
            dao.getAllWordsFlow()
        } else {
            dao.getWordsByLanguageFlow(language)
        }
    }

    fun addWord(word: String, language: String = "ALL", shortcut: String = "") {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return

        repositoryScope.launch {
            val existing = dao.findByWord(cleanWord)
            if (existing != null) {
                dao.update(existing.copy(language = language, shortcut = shortcut.trim(), timestamp = System.currentTimeMillis()))
            } else {
                dao.insert(
                    UserDictionaryEntity(
                        word = cleanWord,
                        language = language,
                        shortcut = shortcut.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateWord(entity: UserDictionaryEntity) {
        val cleanWord = entity.word.trim()
        if (cleanWord.isBlank()) return

        repositoryScope.launch {
            dao.update(entity.copy(word = cleanWord, shortcut = entity.shortcut.trim()))
        }
    }

    fun deleteWord(entity: UserDictionaryEntity) {
        repositoryScope.launch {
            dao.delete(entity)
        }
    }

    fun deleteById(id: Long) {
        repositoryScope.launch {
            dao.deleteById(id)
        }
    }

    fun clearAll() {
        repositoryScope.launch {
            dao.clearAll()
        }
    }

    fun resetToDefaults() {
        repositoryScope.launch {
            dao.clearAll()
            dao.insertAll(DEFAULT_WORDS)
        }
    }

    companion object {
        val DEFAULT_WORDS = listOf(
            UserDictionaryEntity(word = "Kotlin", language = "ALL", shortcut = "kt"),
            UserDictionaryEntity(word = "Jetpack Compose", language = "ALL", shortcut = "jc"),
            UserDictionaryEntity(word = "Kubernetes", language = "ALL", shortcut = "k8s"),
            UserDictionaryEntity(word = "Microserviços", language = "pt_br", shortcut = ""),
            UserDictionaryEntity(word = "Backend", language = "ALL", shortcut = ""),
            UserDictionaryEntity(word = "Frontend", language = "ALL", shortcut = ""),
            UserDictionaryEntity(word = "Fullstack", language = "ALL", shortcut = ""),
            UserDictionaryEntity(word = "Deploy", language = "ALL", shortcut = ""),
            UserDictionaryEntity(word = "Pull Request", language = "ALL", shortcut = "pr"),
            UserDictionaryEntity(word = "Code Review", language = "ALL", shortcut = "cr")
        )

        @Volatile
        private var INSTANCE: UserDictionaryRepository? = null

        fun getInstance(context: Context): UserDictionaryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserDictionaryRepository(
                    KeyboardDatabase.getInstance(context).userDictionaryDao()
                ).also { INSTANCE = it }
            }
        }
    }
}
