package com.example.keyboard.reader

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object FloatingReaderPreferences {
    const val FILE_NAME = "floating_reader_preferences"
    const val KEY_ENABLED = "floating_reader_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = ComponentName(
            context,
            FloatingReaderAccessibilityService::class.java
        ).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
