package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.data.db.DailyStatsEntity
import com.example.keyboard.data.db.KeyFrequencyEntity
import com.example.keyboard.data.db.KeyboardDatabase
import com.example.keyboard.data.db.KeyboardStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DayStatsSummary(
    val dayEpoch: Long,
    val dayLabel: String, // "Seg", "Ter", "Qua"...
    val fullDateLabel: String,
    val wordsTyped: Long,
    val charsTyped: Long,
    val activeMinutes: Int
)

data class WeeklyReportSummary(
    val dailyList: List<DayStatsSummary> = emptyList(),
    val totalWeeklyWords: Long = 0L,
    val dailyAverageWords: Long = 0L,
    val bestDayLabel: String = "-",
    val bestDayWords: Long = 0L
)

data class StatsSummary(
    val totalWords: Long = 0L,
    val totalChars: Long = 0L,
    val averageWpm: Int = 0,
    val currentStreakDays: Int = 0,
    val mostUsedKey: String = "-",
    val mostUsedKeyCount: Long = 0L
)

class KeyboardStatsRepository private constructor(context: Context) {

    private val db = KeyboardDatabase.getInstance(context)
    private val statsDao = db.statsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastKeyTimestamp: Long = 0L

    val statsSummary: Flow<StatsSummary> = combine(
        statsDao.getStatsFlow(),
        statsDao.getTopKeysFlow()
    ) { stats, topKeys ->
        val entity = stats ?: KeyboardStatsEntity(id = 1)
        val topKey = topKeys.firstOrNull()

        val activeDurationMs = entity.totalActiveTypingDurationMs
        val wpm = if (activeDurationMs > 3000L && entity.totalWordsTyped > 0) {
            val minutes = activeDurationMs.toDouble() / 60000.0
            (entity.totalWordsTyped / minutes).toInt().coerceIn(1, 200)
        } else if (entity.totalWordsTyped > 0) {
            // Estimated baseline for early typing
            (entity.totalWordsTyped * 3).toInt().coerceIn(15, 65)
        } else {
            0
        }

        StatsSummary(
            totalWords = entity.totalWordsTyped,
            totalChars = entity.totalCharsTyped,
            averageWpm = wpm,
            currentStreakDays = entity.currentStreakDays,
            mostUsedKey = topKey?.keyChar ?: "-",
            mostUsedKeyCount = topKey?.count ?: 0L
        )
    }

    val topKeys: Flow<List<KeyFrequencyEntity>> = statsDao.getTopKeysFlow()

    val weeklyReport: Flow<WeeklyReportSummary> = statsDao.getWeeklyStatsFlow(
        startDayEpoch = getDayEpoch(System.currentTimeMillis()) - 6
    ).map { dbDays ->
        val currentEpoch = getDayEpoch(System.currentTimeMillis())
        val dayFormat = SimpleDateFormat("EEE", Locale("pt", "BR"))
        val fullFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val dayMap = dbDays.associateBy { it.dayEpoch }
        val last7Days = (6 downTo 0).map { offset ->
            val epoch = currentEpoch - offset
            val timeMillis = epoch * 24 * 60 * 60 * 1000L
            val date = Date(timeMillis)
            val entity = dayMap[epoch] ?: DailyStatsEntity(dayEpoch = epoch)

            DayStatsSummary(
                dayEpoch = epoch,
                dayLabel = dayFormat.format(date).replace(".", "").uppercase(),
                fullDateLabel = fullFormat.format(date),
                wordsTyped = entity.wordsTyped,
                charsTyped = entity.charsTyped,
                activeMinutes = (entity.activeDurationMs / 60000L).toInt()
            )
        }

        val totalWords = last7Days.sumOf { it.wordsTyped }
        val bestDay = last7Days.maxByOrNull { it.wordsTyped }

        WeeklyReportSummary(
            dailyList = last7Days,
            totalWeeklyWords = totalWords,
            dailyAverageWords = totalWords / 7,
            bestDayLabel = if (bestDay != null && bestDay.wordsTyped > 0) "${bestDay.dayLabel} (${bestDay.fullDateLabel})" else "-",
            bestDayWords = bestDay?.wordsTyped ?: 0L
        )
    }

    fun recordTypedChar(char: String, isSecure: Boolean) {
        if (isSecure || char.isEmpty()) return

        scope.launch {
            val now = System.currentTimeMillis()
            val timeDiff = if (lastKeyTimestamp > 0L) (now - lastKeyTimestamp) else 0L
            lastKeyTimestamp = now

            val addedDuration = if (timeDiff in 50L..4000L) timeDiff else 500L

            val currentStats = statsDao.getStatsDirect() ?: KeyboardStatsEntity(id = 1)

            val currentDayEpoch = getDayEpoch(now)
            val updatedStreak = calculateStreak(currentStats.lastActiveDayEpoch, currentDayEpoch, currentStats.currentStreakDays)

            val updatedStats = currentStats.copy(
                totalCharsTyped = currentStats.totalCharsTyped + 1,
                totalActiveTypingDurationMs = currentStats.totalActiveTypingDurationMs + addedDuration,
                lastActiveDayEpoch = currentDayEpoch,
                currentStreakDays = updatedStreak
            )
            statsDao.insertOrUpdateStats(updatedStats)

            // Update daily stats
            val existingDaily = statsDao.getDailyStatsDirect(currentDayEpoch) ?: DailyStatsEntity(dayEpoch = currentDayEpoch)
            statsDao.insertOrUpdateDailyStats(
                existingDaily.copy(
                    charsTyped = existingDaily.charsTyped + 1,
                    activeDurationMs = existingDaily.activeDurationMs + addedDuration
                )
            )

            // Update key frequency for letters, digits, and primary symbols
            val normalizedChar = char.uppercase()
            val existingFreq = statsDao.getKeyFrequency(normalizedChar)
            val newCount = (existingFreq?.count ?: 0L) + 1
            statsDao.insertOrUpdateKeyFrequency(KeyFrequencyEntity(keyChar = normalizedChar, count = newCount))
        }
    }

    fun recordTypedWord(wordCount: Int = 1, isSecure: Boolean) {
        if (isSecure || wordCount <= 0) return

        scope.launch {
            val now = System.currentTimeMillis()
            val currentStats = statsDao.getStatsDirect() ?: KeyboardStatsEntity(id = 1)
            val currentDayEpoch = getDayEpoch(now)
            val updatedStreak = calculateStreak(currentStats.lastActiveDayEpoch, currentDayEpoch, currentStats.currentStreakDays)

            val updatedStats = currentStats.copy(
                totalWordsTyped = currentStats.totalWordsTyped + wordCount,
                lastActiveDayEpoch = currentDayEpoch,
                currentStreakDays = updatedStreak
            )
            statsDao.insertOrUpdateStats(updatedStats)

            // Update daily stats
            val existingDaily = statsDao.getDailyStatsDirect(currentDayEpoch) ?: DailyStatsEntity(dayEpoch = currentDayEpoch)
            statsDao.insertOrUpdateDailyStats(
                existingDaily.copy(
                    wordsTyped = existingDaily.wordsTyped + wordCount
                )
            )
        }
    }

    fun resetAllStats() {
        scope.launch {
            statsDao.resetAllStats()
        }
    }

    private fun getDayEpoch(timestamp: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    private fun calculateStreak(lastDayEpoch: Long, currentDayEpoch: Long, currentStreak: Int): Int {
        if (lastDayEpoch == 0L) return 1
        return when (currentDayEpoch - lastDayEpoch) {
            0L -> if (currentStreak == 0) 1 else currentStreak
            1L -> currentStreak + 1
            else -> 1 // Streak broken or new streak
        }
    }

    companion object {
        @Volatile
        private var instance: KeyboardStatsRepository? = null

        fun getInstance(context: Context): KeyboardStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: KeyboardStatsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
