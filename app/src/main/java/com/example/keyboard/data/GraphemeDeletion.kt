package com.example.keyboard.data

/**
 * Finds the UTF-16 length of the final user-perceived character in a text prefix.
 * It keeps combining marks, variation selectors, emoji modifiers, regional-indicator
 * pairs, tag sequences and ZWJ emoji together so backspace never leaves a broken emoji.
 */
object GraphemeDeletion {
    fun previousClusterLength(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        val value = text.toString()
        var start = previousCodePointStart(value, value.length)

        // Keep trailing combining marks, emoji modifiers, variation selectors and emoji tags.
        start = consumeExtensionsBefore(value, start)

        // A malformed/in-progress sequence ending in ZWJ should still be removed safely.
        if (codePointAt(value, start) == ZERO_WIDTH_JOINER && start > 0) {
            start = consumeExtensionsBefore(value, previousCodePointStart(value, start))
        }

        // Join all preceding bases in an emoji ZWJ chain (e.g. family/profession emojis).
        while (start > 0) {
            val preceding = previousCodePointStart(value, start)
            if (codePointAt(value, preceding) != ZERO_WIDTH_JOINER) break
            start = consumeExtensionsBefore(value, previousCodePointStart(value, preceding))
        }

        // CRLF is one grapheme cluster.
        if (codePointAt(value, start) == LINE_FEED && start > 0) {
            val preceding = previousCodePointStart(value, start)
            if (codePointAt(value, preceding) == CARRIAGE_RETURN) start = preceding
        }

        // Pair regional indicators from the beginning of each run (flags).
        if (isRegionalIndicator(codePointAt(value, start))) {
            var runStart = start
            var count = 0
            while (runStart > 0) {
                val preceding = previousCodePointStart(value, runStart)
                if (!isRegionalIndicator(codePointAt(value, preceding))) break
                runStart = preceding
                count++
            }
            if (count % 2 == 1) start = runStart
        }

        return value.length - start
    }

    private fun consumeExtensionsBefore(value: String, initialStart: Int): Int {
        var start = initialStart
        while (start > 0 && isExtension(codePointAt(value, start))) {
            start = previousCodePointStart(value, start)
        }
        return start
    }

    private fun previousCodePointStart(value: String, end: Int): Int =
        if (end <= 0) 0 else end - Character.charCount(Character.codePointBefore(value, end))

    private fun codePointAt(value: String, index: Int): Int =
        if (index < 0 || index >= value.length) -1 else Character.codePointAt(value, index)

    private fun isExtension(codePoint: Int): Boolean {
        if (codePoint < 0) return false
        val type = Character.getType(codePoint)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint in 0xE0100..0xE01EF ||
            codePoint in 0x1F3FB..0x1F3FF ||
            codePoint in 0xE0020..0xE007F
    }

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in 0x1F1E6..0x1F1FF

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val CARRIAGE_RETURN = 0x000D
    private const val LINE_FEED = 0x000A
}
