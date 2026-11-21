package com.example.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDictionaryDao {

    @Query("SELECT * FROM user_dictionary ORDER BY word COLLATE NOCASE ASC")
    fun getAllWordsFlow(): Flow<List<UserDictionaryEntity>>

    @Query("SELECT * FROM user_dictionary WHERE language = :language OR language = 'ALL' ORDER BY word COLLATE NOCASE ASC")
    fun getWordsByLanguageFlow(language: String): Flow<List<UserDictionaryEntity>>

    @Query("SELECT * FROM user_dictionary")
    suspend fun getAllWords(): List<UserDictionaryEntity>

    @Query("SELECT * FROM user_dictionary WHERE word = :word COLLATE NOCASE LIMIT 1")
    suspend fun findByWord(word: String): UserDictionaryEntity?

    @Query("SELECT COUNT(*) FROM user_dictionary")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wordEntity: UserDictionaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<UserDictionaryEntity>)

    @Update
    suspend fun update(wordEntity: UserDictionaryEntity)

    @Delete
    suspend fun delete(wordEntity: UserDictionaryEntity)

    @Query("DELETE FROM user_dictionary WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_dictionary")
    suspend fun clearAll()
}
