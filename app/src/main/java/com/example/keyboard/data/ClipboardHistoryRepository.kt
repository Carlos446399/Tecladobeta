package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.data.db.ClipboardItemEntity
import com.example.keyboard.data.db.KeyboardDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ClipboardHistoryRepository private constructor(context: Context) {

    private val db = KeyboardDatabase.getInstance(context)
    private val clipboardDao = db.clipboardDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val recentClips: Flow<List<ClipboardItemEntity>> = clipboardDao.getRecentClipsFlow()

    fun addClip(text: String, isSecure: Boolean) {
        if (isSecure) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            clipboardDao.addClipAndTrim(trimmed)
        }
    }

    fun deleteClip(id: Long) {
        scope.launch {
            clipboardDao.deleteClipById(id)
        }
    }

    fun clearAll() {
        scope.launch {
            clipboardDao.clearAllClips()
        }
    }

    companion object {
        @Volatile
        private var instance: ClipboardHistoryRepository? = null

        fun getInstance(context: Context): ClipboardHistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: ClipboardHistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
