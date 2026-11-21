package com.example.keyboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KeyboardStatsEntity::class,
        DailyStatsEntity::class,
        KeyFrequencyEntity::class,
        ClipboardItemEntity::class,
        TextShortcutEntity::class,
        AppPhraseEntity::class,
        UserDictionaryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KeyboardDatabase : RoomDatabase() {

    abstract fun statsDao(): StatsDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun textShortcutDao(): TextShortcutDao
    abstract fun appPhraseDao(): AppPhraseDao
    abstract fun userDictionaryDao(): UserDictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: KeyboardDatabase? = null

        fun getInstance(context: Context): KeyboardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeyboardDatabase::class.java,
                    "custom_keyboard_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
