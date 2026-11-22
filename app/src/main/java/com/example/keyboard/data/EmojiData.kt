package com.example.keyboard.data

import android.content.Context

/** Offline emoji catalogue derived from Unicode Emoji 18.0 emoji-test.txt. */
data class EmojiCategory(
    val id: String,
    val name: String,
    val icon: String,
    val emojis: List<String>
)

object EmojiData {
    private const val ASSET_NAME = "emoji-test-18.0.txt"

    private val groupInfo = linkedMapOf(
        "Smileys & Emotion" to Triple("smileys", "Rostos", "😀"),
        "People & Body" to Triple("people", "Pessoas", "👋"),
        "Animals & Nature" to Triple("animals", "Animais", "🐶"),
        "Food & Drink" to Triple("food", "Comidas", "🍔"),
        "Travel & Places" to Triple("travel", "Viagem", "🚗"),
        "Activities" to Triple("activities", "Atividades", "⚽"),
        "Objects" to Triple("objects", "Objetos", "💡"),
        "Symbols" to Triple("symbols", "Símbolos", "❤️"),
        "Flags" to Triple("flags", "Bandeiras", "🏳️")
    )

    fun load(context: Context): List<EmojiCategory> =
        context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).useLines(::parseLines)

    internal fun parseLines(lines: Sequence<String>): List<EmojiCategory> {
        val grouped = linkedMapOf<String, LinkedHashSet<String>>()
        var group: String? = null
        for (line in lines) {
            if (line.startsWith("# group: ")) {
                group = line.substringAfter("# group: ").trim()
                continue
            }
            if (!line.contains("; fully-qualified")) continue
            val currentGroup = group ?: continue
            val emoji = line.substringAfter("#", "").trim().substringBefore(' ')
            if (emoji.isNotEmpty() && currentGroup in groupInfo) {
                grouped.getOrPut(currentGroup) { linkedSetOf() }.add(emoji)
            }
        }
        return groupInfo.mapNotNull { (sourceGroup, metadata) ->
            val emojis = grouped[sourceGroup].orEmpty().toList()
            if (emojis.isEmpty()) null else EmojiCategory(
                id = metadata.first,
                name = metadata.second,
                icon = metadata.third,
                emojis = emojis
            )
        }
    }
}
