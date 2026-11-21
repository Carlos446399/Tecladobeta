package com.example.keyboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM keyboard_stats WHERE id = 1")
    fun getStatsFlow(): Flow<KeyboardStatsEntity?>

    @Query("SELECT * FROM keyboard_stats WHERE id = 1")
    suspend fun getStatsDirect(): KeyboardStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: KeyboardStatsEntity)

    @Query("SELECT * FROM key_frequencies ORDER BY count DESC LIMIT 10")
    fun getTopKeysFlow(): Flow<List<KeyFrequencyEntity>>

    @Query("SELECT * FROM key_frequencies ORDER BY count DESC LIMIT 1")
    suspend fun getMostUsedKeyDirect(): KeyFrequencyEntity?

    @Query("SELECT * FROM key_frequencies WHERE keyChar = :keyChar")
    suspend fun getKeyFrequency(keyChar: String): KeyFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKeyFrequency(keyFreq: KeyFrequencyEntity)

    @Query("SELECT * FROM daily_stats WHERE dayEpoch = :dayEpoch")
    suspend fun getDailyStatsDirect(dayEpoch: Long): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE dayEpoch >= :startDayEpoch ORDER BY dayEpoch ASC")
    fun getWeeklyStatsFlow(startDayEpoch: Long): Flow<List<DailyStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStats(dailyStats: DailyStatsEntity)

    @Query("DELETE FROM daily_stats")
    suspend fun clearDailyStats()

    @Query("DELETE FROM keyboard_stats")
    suspend fun clearStats()

    @Query("DELETE FROM key_frequencies")
    suspend fun clearKeyFrequencies()

    @Transaction
    suspend fun resetAllStats() {
        clearStats()
        clearKeyFrequencies()
        clearDailyStats()
        insertOrUpdateStats(KeyboardStatsEntity(id = 1))
    }
}
