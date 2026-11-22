package com.example

import com.example.keyboard.data.AppPhrasesRepository
import com.example.keyboard.data.EmojiData
import com.example.keyboard.data.GraphemeDeletion
import com.example.keyboard.data.UnknownWordLearningTracker
import com.example.keyboard.data.WordDictionary
import com.example.keyboard.model.AccentColorType
import com.example.keyboard.model.KeyShapeStyle
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardThemeColorsFactory
import com.example.keyboard.model.KeyboardThemeType
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexiKeyFeatureUnitTest {

    @Test
    fun testLevenshteinDistanceCalculation() {
        assertEquals(0, WordDictionary.levenshteinDistance("teste", "teste"))
        assertEquals(1, WordDictionary.levenshteinDistance("obrigdo", "obrigado"))
        assertEquals(1, WordDictionary.levenshteinDistance("tste", "teste"))
        assertEquals(1, WordDictionary.levenshteinDistance("testes", "teste"))
        assertEquals(1, WordDictionary.levenshteinDistance("casa", "cama"))
        assertEquals(3, WordDictionary.levenshteinDistance("casa", "carro"))
    }

    @Test
    fun testAutocorrectOneLetterDifference() {
        // "obrigdo" should autocorrect to "obrigado" in PT_BR
        val correctedPt = WordDictionary.getAutocorrectWord("obrigdo", KeyboardLanguage.PT_BR)
        assertEquals("obrigado", correctedPt)

        // Exact match should NOT autocorrect (returns null because it's already in dictionary)
        val exactPt = WordDictionary.getAutocorrectWord("obrigado", KeyboardLanguage.PT_BR)
        assertNull(exactPt)

        // "hve" can be "have" or "he" after one edit; keep it unchanged and offer suggestions.
        assertNull(WordDictionary.getAutocorrectWord("hve", KeyboardLanguage.EN_US))
        assertTrue(WordDictionary.getSuggestions("hve", KeyboardLanguage.EN_US).contains("have"))

        // Words with >= 3 edits should NOT autocorrect
        val farWord = WordDictionary.getAutocorrectWord("xyzabc", KeyboardLanguage.PT_BR)
        assertNull(farWord)
    }

    @Test
    fun testAutocorrectCasePreservation() {
        // Capitalized first letter
        val capitalized = WordDictionary.getAutocorrectWord("Obrigdo", KeyboardLanguage.PT_BR)
        assertEquals("Obrigado", capitalized)

        // ALL CAPS
        val allCaps = WordDictionary.getAutocorrectWord("OBRIGDO", KeyboardLanguage.PT_BR)
        assertEquals("OBRIGADO", allCaps)
    }

    @Test
    fun testAccentRestorationAndAmbiguousWords() {
        assertEquals("você", WordDictionary.getAutocorrectWord("voce", KeyboardLanguage.PT_BR))
        assertEquals("não", WordDictionary.getAutocorrectWord("nao", KeyboardLanguage.PT_BR))
        assertNull("avo pode significar avó ou avô; não deve ser adivinhado", WordDictionary.getAutocorrectWord("avo", KeyboardLanguage.PT_BR))
    }

    @Test
    fun testPrefixAndContextualSuggestions() {
        assertTrue(WordDictionary.getSuggestions("vo", KeyboardLanguage.PT_BR).contains("você"))
        assertEquals("dia", WordDictionary.getSuggestions("", KeyboardLanguage.PT_BR, previousWord = "bom").first())
        assertTrue(WordDictionary.getSuggestions("d", KeyboardLanguage.PT_BR, previousWord = "bom").contains("dia"))
    }

    @Test
    fun testLanguageDetectionFromWords() {
        // Clear Portuguese stream
        val ptWords = listOf("olá", "como", "você", "está", "obrigado")
        val detectedPt = WordDictionary.detectLanguageFromWords(ptWords)
        assertEquals(KeyboardLanguage.PT_BR, detectedPt)

        // Clear English stream
        val enWords = listOf("hello", "how", "are", "you", "thanks")
        val detectedEn = WordDictionary.detectLanguageFromWords(enWords)
        assertEquals(KeyboardLanguage.EN_US, detectedEn)

        // Mixed/Ambiguous stream
        val ambiguous = listOf("123", "xyz")
        val detectedAmbiguous = WordDictionary.detectLanguageFromWords(ambiguous)
        assertNull(detectedAmbiguous)
    }

    @Test
    fun testAppPhrasesCategoryMapping() {
        assertEquals(
            AppPhrasesRepository.CATEGORY_MESSAGING,
            AppPhrasesRepository.getCategoryForPackage("com.whatsapp")
        )
        assertEquals(
            AppPhrasesRepository.CATEGORY_MESSAGING,
            AppPhrasesRepository.getCategoryForPackage("org.telegram.messenger")
        )
        assertEquals(
            AppPhrasesRepository.CATEGORY_EMAIL,
            AppPhrasesRepository.getCategoryForPackage("com.google.android.gm")
        )
        assertEquals(
            AppPhrasesRepository.CATEGORY_EMAIL,
            AppPhrasesRepository.getCategoryForPackage("com.microsoft.office.outlook")
        )
        assertEquals(
            AppPhrasesRepository.CATEGORY_GENERAL,
            AppPhrasesRepository.getCategoryForPackage("com.random.app")
        )
    }

    @Test
    fun testUserDictionaryCustomWordsSupport() {
        // Given custom words
        val customWords = listOf("Kubernetes", "Kotlin", "Microserviços")
        WordDictionary.setUserCustomWords(customWords)

        // Verify isWordInDictionary recognizes custom words
        assertTrue(WordDictionary.isWordInDictionary("Kubernetes", KeyboardLanguage.PT_BR))
        assertTrue(WordDictionary.isWordInDictionary("kubernetes", KeyboardLanguage.EN_US))
        assertTrue(WordDictionary.isWordInDictionary("microservicos", KeyboardLanguage.PT_BR))

        // Exact custom word should NOT autocorrect (it's recognized as valid)
        assertNull(WordDictionary.getAutocorrectWord("Kubernetes", KeyboardLanguage.PT_BR))

        // Typo of 1 char close to custom word should correct to custom word
        val correctedCustom = WordDictionary.getAutocorrectWord("Kubernets", KeyboardLanguage.PT_BR)
        assertEquals("Kubernetes", correctedCustom)

        // Custom words should be suggested first
        val suggestions = WordDictionary.getSuggestions("Kube", KeyboardLanguage.PT_BR)
        assertTrue(suggestions.contains("Kubernetes"))
    }

    @Test
    fun testEmojiParserKeepsOnlyFullyQualifiedSequencesByGroup() {
        val categories = EmojiData.parseLines(
            sequenceOf(
                "# group: Smileys & Emotion",
                "1F600 ; fully-qualified # 😀 E1.0 grinning face",
                "1F600 ; minimally-qualified # 😀 E1.0 grinning face",
                "# group: People & Body",
                "1F9D1 1F3FD 200D 1F4BB ; fully-qualified # 🧑🏽‍💻 E4.0 technologist"
            )
        )

        assertEquals(listOf("Rostos", "Pessoas"), categories.map { it.name })
        assertEquals(listOf("😀"), categories[0].emojis)
        assertEquals(listOf("🧑🏽‍💻"), categories[1].emojis)
    }

    @Test
    fun testUnknownWordLearningNeedsTwoPrivateSafeConfirmations() {
        val tracker = UnknownWordLearningTracker()
        assertTrue(!tracker.recordAttempt("café", isKnown = false, isSuppressed = false))
        assertTrue(!tracker.recordAttempt("CAFE", isKnown = false, isSuppressed = true))
        assertTrue(!tracker.recordAttempt("cafe", isKnown = true, isSuppressed = false))
        assertTrue(tracker.recordAttempt("CAFE", isKnown = false, isSuppressed = false))
        assertTrue(!tracker.recordAttempt("café", isKnown = false, isSuppressed = false))
    }

    @Test
    fun testHoneycombThemeUsesDarkGoldPaletteAndHexagonalShape() {
        val colors = KeyboardThemeColorsFactory.getColors(KeyboardThemeType.HONEYCOMB, AccentColorType.AMBER)
        assertEquals(Color(0xFF090A0D), colors.background)
        assertEquals(Color(0xFFD5A84C).copy(alpha = 0.75f), colors.keyBorderColor)
        assertEquals(-1, KeyShapeStyle.HEXAGON.cornerRadiusDp)
    }

    @Test
    fun testBackspaceDeletesWholeEmojiGrapheme() {
        val examples = listOf(
            "a🙂" to "🙂",
            "oi 👍🏽" to "👍🏽",
            "x 👨‍👩‍👧‍👦" to "👨‍👩‍👧‍👦",
            "x 🇧🇷" to "🇧🇷",
            "x 1️⃣" to "1️⃣"
        )

        examples.forEach { (text, grapheme) ->
            assertEquals(grapheme.length, GraphemeDeletion.previousClusterLength(text))
        }
    }
}
