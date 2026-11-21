package com.example.keyboard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keyboard_stats")
data class KeyboardStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalWordsTyped: Long = 0L,
    val totalCharsTyped: Long = 0L,
    val totalActiveTypingDurationMs: Long = 0L,
    val lastActiveDayEpoch: Long = 0L,
    val currentStreakDays: Int = 0
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val dayEpoch: Long,
    val wordsTyped: Long = 0L,
    val charsTyped: Long = 0L,
    val activeDurationMs: Long = 0L
)

@Entity(tableName = "key_frequencies")
data class KeyFrequencyEntity(
    @PrimaryKey val keyChar: String,
    val count: Long = 0L
)

@Entity(tableName = "clipboard_history")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "text_shortcuts")
data class TextShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trigger: String,
    val expansion: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "app_phrases")
data class AppPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appCategory: String, // "messaging", "email", "general"
    val packageName: String? = null,
    val phrase: String,
    val isCustom: Boolean = false
)

@Entity(tableName = "user_dictionary")
data class UserDictionaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val word: String,
    val language: String = "ALL", // "ALL", "pt_br", "en_us"
    val shortcut: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

