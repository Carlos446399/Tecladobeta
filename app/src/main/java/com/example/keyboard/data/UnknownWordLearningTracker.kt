package com.example.keyboard.data

/** Tracks repeated unknown words only in memory; callers decide when persistence is safe. */
class UnknownWordLearningTracker(
    private val confirmationsRequired: Int = 2,
    private val maxTrackedWords: Int = 128
) {
    private val attempts = LinkedHashMap<String, Int>()

    init {
        require(confirmationsRequired >= 2)
        require(maxTrackedWords > 0)
    }

    @Synchronized
    fun recordAttempt(word: String, isKnown: Boolean, isSuppressed: Boolean): Boolean {
        if (isKnown || isSuppressed) return false
        val key = WordDictionary.normalize(word.trim())
        if (key.isBlank() || key.none(Char::isLetter)) return false

        val nextCount = (attempts[key] ?: 0) + 1
        if (nextCount >= confirmationsRequired) {
            attempts.remove(key)
            return true
        }
        attempts[key] = nextCount
        while (attempts.size > maxTrackedWords) {
            attempts.remove(attempts.keys.first())
        }
        return false
    }

    @Synchronized
    fun clear() = attempts.clear()
}
