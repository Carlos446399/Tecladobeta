package com.example.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPhraseDao {

    @Query("SELECT * FROM app_phrases ORDER BY id ASC")
    fun getAllPhrasesFlow(): Flow<List<AppPhraseEntity>>

    @Query("SELECT * FROM app_phrases WHERE appCategory = :category ORDER BY id ASC")
    fun getPhrasesByCategoryFlow(category: String): Flow<List<AppPhraseEntity>>

    @Query("SELECT * FROM app_phrases WHERE appCategory = :category ORDER BY id ASC")
    suspend fun getPhrasesByCategory(category: String): List<AppPhraseEntity>

    @Query("SELECT COUNT(*) FROM app_phrases")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: AppPhraseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<AppPhraseEntity>)

    @Update
    suspend fun update(phrase: AppPhraseEntity)

    @Delete
    suspend fun delete(phrase: AppPhraseEntity)

    @Query("DELETE FROM app_phrases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM app_phrases")
    suspend fun clearAll()
}
