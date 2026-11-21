package com.example.keyboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentClipsFlow(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentClipsDirect(): List<ClipboardItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipboardItemEntity): Long

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteClipById(id: Long)

    @Query("DELETE FROM clipboard_history WHERE text = :text")
    suspend fun deleteClipByText(text: String)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAllClips()

    @Query("DELETE FROM clipboard_history WHERE id NOT IN (SELECT id FROM clipboard_history ORDER BY timestamp DESC LIMIT 20)")
    suspend fun trimOldClips()

    @Transaction
    suspend fun addClipAndTrim(text: String) {
        // Avoid duplicate consecutive identical text or remove old copy first
        deleteClipByText(text)
        insertClip(ClipboardItemEntity(text = text, timestamp = System.currentTimeMillis()))
        trimOldClips()
    }
}
