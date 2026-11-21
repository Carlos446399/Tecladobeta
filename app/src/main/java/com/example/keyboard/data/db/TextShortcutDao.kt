package com.example.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TextShortcutDao {

    @Query("SELECT * FROM text_shortcuts ORDER BY trigger ASC")
    fun getAllShortcutsFlow(): Flow<List<TextShortcutEntity>>

    @Query("SELECT * FROM text_shortcuts WHERE isEnabled = 1")
    suspend fun getEnabledShortcuts(): List<TextShortcutEntity>

    @Query("SELECT * FROM text_shortcuts WHERE trigger = :trigger COLLATE NOCASE AND isEnabled = 1 LIMIT 1")
    suspend fun findByTrigger(trigger: String): TextShortcutEntity?

    @Query("SELECT COUNT(*) FROM text_shortcuts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: TextShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<TextShortcutEntity>)

    @Update
    suspend fun update(shortcut: TextShortcutEntity)

    @Delete
    suspend fun delete(shortcut: TextShortcutEntity)

    @Query("DELETE FROM text_shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM text_shortcuts")
    suspend fun clearAll()
}
