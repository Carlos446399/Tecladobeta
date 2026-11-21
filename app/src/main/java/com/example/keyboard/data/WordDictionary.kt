package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.model.KeyboardLanguage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import kotlin.math.min

object WordDictionary {

    @Volatile
    private var loadedPortugueseWords: List<String> = emptyList()
    @Volatile
    private var loadedEnglishWords: List<String> = emptyList()
    @Volatile
    private var loadedPortugueseNormalizedSet: Set<String> = emptySet()
    @Volatile
    private var loadedEnglishNormalizedSet: Set<String> = emptySet()
    @Volatile
    private var loadedPortugueseExactSet: Set<String> = emptySet()
    @Volatile
    private var loadedEnglishExactSet: Set<String> = emptySet()
    @Volatile
    private var loadedPortugueseAccentMap: Map<String, String> = emptyMap()
    @Volatile
    private var loadedEnglishAccentMap: Map<String, String> = emptyMap()

    // Prefix index: map from 2-letter normalized prefix to list of words for O(1) prefix lookup
    @Volatile
    private var ptPrefixIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var enPrefixIndex: Map<String, List<String>> = emptyMap()

    @Volatile
    var isInitialized = false
        private set

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return

            val ptWords = ArrayList<String>(35000)
            val ptNormSet = HashSet<String>(35000)
            val ptExactSet = HashSet<String>(35000)
            val ptAccentMap = HashMap<String, String>(15000)
            val ptIndex = HashMap<String, MutableList<String>>()

            try {
                loadLanguageAsset(context, "dict_pt_br.txt", ptWords, ptNormSet, ptExactSet, ptAccentMap, ptIndex)
                loadedPortugueseWords = ptWords
                loadedPortugueseNormalizedSet = ptNormSet
                loadedPortugueseExactSet = ptExactSet
                loadedPortugueseAccentMap = ptAccentMap
                ptPrefixIndex = ptIndex
            } catch (_: Exception) {}

            val enWords = ArrayList<String>(25000)
            val enNormSet = HashSet<String>(25000)
            val enExactSet = HashSet<String>(25000)
            val enAccentMap = HashMap<String, String>(5000)
            val enIndex = HashMap<String, MutableList<String>>()

            try {
                loadLanguageAsset(context, "dict_en_us.txt", enWords, enNormSet, enExactSet, enAccentMap, enIndex)
                loadedEnglishWords = enWords
                loadedEnglishNormalizedSet = enNormSet
                loadedEnglishExactSet = enExactSet
                loadedEnglishAccentMap = enAccentMap
                enPrefixIndex = enIndex
            } catch (_: Exception) {}

            isInitialized = true
        }
    }

    private fun loadLanguageAsset(
        context: Context,
        fileName: String,
        targetList: MutableList<String>,
        targetNormSet: MutableSet<String>,
        targetExactSet: MutableSet<String>,
        targetAccentMap: MutableMap<String, String>,
        prefixIndex: MutableMap<String, MutableList<String>>
    ) {
        val inputStream = context.assets.open(fileName)
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            lines.forEach { line ->
                val w = line.trim()
                if (w.isNotEmpty()) {
                    val lower = w.lowercase()
                    targetList.add(lower)
                    targetExactSet.add(lower)
                    val norm = normalize(lower)
                    targetNormSet.add(norm)
                    if (!targetAccentMap.containsKey(norm)) {
                        targetAccentMap[norm] = lower
                    }
                    if (norm.length >= 2) {
                        val key = norm.substring(0, 2)
                        prefixIndex.getOrPut(key) { mutableListOf() }.add(lower)
                    }
                }
            }
        }
    }

    fun normalize(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    val fallbackPortugueseWords = listOf(
        "você", "como", "está", "tudo", "bem", "muito", "obrigado", "obrigada", "bom", "dia",
        "boa", "tarde", "noite", "olá", "sim", "não", "por", "favor", "onde", "quando",
        "porque", "porquê", "quem", "qual", "coisa", "tempo", "ano", "homem", "mulher",
        "vida", "criança", "mundo", "escola", "trabalho", "casa", "país", "família", "governo",
        "grupo", "problema", "mão", "parte", "lugar", "caso", "semana", "olho", "hora", "ponto",
        "água", "fazer", "dizer", "poder", "ver", "dar", "saber", "querer", "ficar", "deixar",
        "passar", "achar", "falar", "levar", "trazer", "ouvir", "entender", "lembrar", "esperar",
        "grande", "novo", "primeiro", "último", "longo", "próprio", "outro", "velho", "certo",
        "alto", "diferente", "pequeno", "importante", "social", "nacional", "agora", "hoje",
        "amanhã", "ontem", "sempre", "nunca", "já", "ainda", "também", "depois", "aqui", "ali",
        "lá", "mais", "menos", "tão", "assim", "quase", "apenas", "então", "claro", "realmente",
        "com", "sem", "para", "sobre", "entre", "contra", "durante", "segundo", "amigo", "amor",
        "coração", "verdade", "certeza", "mensagem", "teclado", "aplicativo", "celular", "foto",
        "vídeo", "música", "cidade", "carro", "estrada", "viagem", "reunião", "projeto", "dinheiro",
        "conta", "comida", "café", "almoço", "jantar", "saúde", "sucesso", "parabéns", "abraço",
        "abraços", "beleza", "legal", "tranquilo", "show", "maravilha", "perfeito", "ótimo", "ótima",
        "excelente", "falou", "valeu", "tranquila", "fácil", "difícil", "rápido", "rápida",
        "preciso", "precisa", "ajuda", "gente", "pessoas", "nada", "mesmo", "mesma",
        "estou", "estamos", "estão", "somos", "será", "tenho", "temos", "têm", "fui", "vamos",
        "chegando", "cheguei", "caminho", "aguardo", "retorno", "disposição", "atenciosamente",
        "computador", "telefone", "informação", "notícia", "momento", "resposta", "pergunta",
        "estudo", "conversa", "amiga", "irmão", "irmã", "mãe", "pai", "filho", "filha",
        "escrever", "digitar", "enviar", "receber", "ajudar", "começar", "terminar", "sair", "entrar",
        "verdadeiro", "simples", "melhor", "pior", "cedo", "tarde", "junto", "juntos", "após", "até"
    )

    val fallbackEnglishWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "hello", "thanks", "thank", "please", "great", "awesome", "perfect", "today", "tomorrow",
        "yesterday", "morning", "night", "keyboard", "android", "custom", "message", "friend",
        "meeting", "project", "attachment", "regards", "sincerely", "waiting", "update", "working",
        "phone", "computer", "information", "question", "answer", "writing", "typing", "send",
        "receive", "start", "finish", "simple", "better", "together", "always", "never", "really"
    )

    val portugueseWords: List<String>
        get() = if (loadedPortugueseWords.isNotEmpty()) loadedPortugueseWords else fallbackPortugueseWords

    val englishWords: List<String>
        get() = if (loadedEnglishWords.isNotEmpty()) loadedEnglishWords else fallbackEnglishWords

    private val portugueseNormalizedSet by lazy {
        fallbackPortugueseWords.map { normalize(it) }.toSet()
    }

    private val englishNormalizedSet by lazy {
        fallbackEnglishWords.map { normalize(it) }.toSet()
    }

    private val userCustomWords = mutableListOf<String>()
    private val userCustomNormalizedSet = mutableSetOf<String>()

    @Synchronized
    fun setUserCustomWords(words: List<String>) {
        userCustomWords.clear()
        userCustomWords.addAll(words)
        userCustomNormalizedSet.clear()
        userCustomNormalizedSet.addAll(words.map { normalize(it) })
    }

    @Synchronized
    fun getUserCustomWords(): List<String> = userCustomWords.toList()

    // Set of Portuguese-exclusive markers (words or letter patterns)
    private val portugueseExclusiveWords = setOf(
        "voce", "você", "como", "esta", "está", "estou", "tudo", "muito", "obrigado", "obrigada",
        "bom", "dia", "boa", "tarde", "noite", "ola", "olá", "sim", "nao", "não", "por", "favor",
        "onde", "quando", "porque", "porquê", "quem", "qual", "coisa", "tempo", "trabalho", "casa",
        "fazer", "saber", "querer", "ficar", "falar", "agora", "hoje", "amanha", "amanhã", "ontem",
        "sempre", "nunca", "tambem", "também", "depois", "aqui", "mais", "menos", "assim", "entao",
        "então", "claro", "amigo", "abraço", "valeu", "beleza", "legal", "tranquilo", "otimo", "ótimo"
    )

    // Set of English-exclusive markers
    private val englishExclusiveWords = setOf(
        "the", "that", "have", "with", "this", "they", "from", "will", "would", "there", "their",
        "what", "about", "which", "when", "make", "like", "time", "just", "know", "people", "into",
        "your", "good", "some", "could", "them", "other", "than", "then", "look", "only", "come",
        "over", "think", "also", "back", "after", "first", "well", "because", "give", "hello",
        "thanks", "thank", "please", "great", "awesome", "today", "tomorrow", "yesterday", "morning"
    )

    fun isWordInDictionary(rawWord: String, language: KeyboardLanguage): Boolean {
        val clean = rawWord.trim().lowercase()
        if (clean.isEmpty()) return true
        val normalized = normalize(clean)

        if (userCustomNormalizedSet.contains(normalized)) {
            return true
        }

        return when (language) {
            KeyboardLanguage.PT_BR -> {
                loadedPortugueseExactSet.contains(clean) ||
                loadedPortugueseNormalizedSet.contains(normalized) ||
                portugueseNormalizedSet.contains(normalized)
            }
            KeyboardLanguage.EN_US -> {
                loadedEnglishExactSet.contains(clean) ||
                loadedEnglishNormalizedSet.contains(normalized) ||
                englishNormalizedSet.contains(normalized)
            }
        }
    }

    /**
     * Checks a word or multi-word phrase and returns the correct spelling and accentuation.
     * E.g. "voce esta bem" -> "você está bem", "obrigado por tudo" -> "obrigado por tudo", "ate amanha" -> "até amanhã"
     */
    fun getPhraseOrWordCorrection(input: String, language: KeyboardLanguage): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val tokens = trimmed.split(Regex("\\s+"))
        if (tokens.size == 1) {
            val single = tokens[0]
            val corrected = getAutocorrectWord(single, language)
            return if (corrected != null && corrected != single) corrected else null
        }

        var anyChanged = false
        val correctedTokens = tokens.map { token ->
            val cleanToken = token.trim()
            val corrected = getAutocorrectWord(cleanToken, language)
            if (corrected != null && corrected != cleanToken) {
                anyChanged = true
                corrected
            } else {
                cleanToken
            }
        }

        return if (anyChanged) correctedTokens.joinToString(" ") else null
    }

    /**
     * Calculates Levenshtein distance between two strings with early exit
     */
    fun levenshteinDistance(s1: String, s2: String, maxAllowed: Int = Int.MAX_VALUE): Int {
        val m = s1.length
        val n = s2.length
        if (kotlin.math.abs(m - n) > maxAllowed) return maxAllowed + 1

        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)

        for (i in 1..m) {
            curr[0] = i
            var minInRow = curr[0]
            val s1Char = s1[i - 1]
            for (j in 1..n) {
                val cost = if (s1Char == s2[j - 1]) 0 else 1
                curr[j] = min(
                    prev[j] + 1, // deletion
                    min(
                        curr[j - 1] + 1, // insertion
                        prev[j - 1] + cost // substitution
                    )
                )
                if (curr[j] < minInRow) minInRow = curr[j]
            }
            if (minInRow > maxAllowed) return maxAllowed + 1
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[n]
    }

    /**
     * Determines whether [rawWord] should be autocorrected upon pressing space/punctuation.
     * Returns the corrected word with proper capitalization if an edit distance <= 1 is found,
     * or null if the word is already valid or no close match exists.
     */
    fun getAutocorrectWord(rawWord: String, language: KeyboardLanguage): String? {
        val clean = rawWord.trim()
        if (clean.length < 2) return null // Don't autocorrect single letters

        val lowerClean = clean.lowercase()
        val normalizedClean = normalize(lowerClean)

        // 0. If it's a learned / user custom word (e.g. "Kubernetes", "Kotlin", "microserviços"), it's valid!
        if (userCustomWords.any { it.lowercase() == lowerClean } || userCustomNormalizedSet.contains(normalizedClean)) {
            return null
        }

        // 1. If it's already an exact word in dictionary (e.g. "xícara", "obrigado", "como", "the"), do not change
        if (isWordInDictionary(clean, language)) {
            // Check if it's already in the dictionary with accent or exact matching
            val exactMatch = when (language) {
                KeyboardLanguage.PT_BR -> loadedPortugueseExactSet.contains(lowerClean) || fallbackPortugueseWords.any { it.lowercase() == lowerClean }
                KeyboardLanguage.EN_US -> loadedEnglishExactSet.contains(lowerClean) || fallbackEnglishWords.any { it.lowercase() == lowerClean }
            }
            if (exactMatch) return null

            // If it matches normalized form but not exact, check accent restoration (e.g. "voce" -> "você", "xicara" -> "xícara")
            val accentMap = if (language == KeyboardLanguage.PT_BR) loadedPortugueseAccentMap else loadedEnglishAccentMap
            val restored = accentMap[normalizedClean] ?: (when (language) {
                KeyboardLanguage.PT_BR -> fallbackPortugueseWords.firstOrNull { normalize(it) == normalizedClean }
                KeyboardLanguage.EN_US -> fallbackEnglishWords.firstOrNull { normalize(it) == normalizedClean }
            })
            if (restored != null && restored != lowerClean) {
                return formatCapitalization(restored, clean)
            }
            return null
        }

        val wordList = when (language) {
            KeyboardLanguage.PT_BR -> portugueseWords
            KeyboardLanguage.EN_US -> englishWords
        }

        // 2. Check for words with edit distance == 1 (or 2 for longer words)
        var bestCandidate: String? = null
        var minDistance = 999
        var bestCandidateMatchesFirstChar = false

        val allCandidates = userCustomWords + wordList
        for (word in allCandidates) {
            val normalizedDictWord = normalize(word)
            val lenDiff = kotlin.math.abs(normalizedDictWord.length - normalizedClean.length)
            if (lenDiff > 2) continue

            val maxAllowedDistance = if (normalizedClean.length >= 5) 2 else 1
            val distance = levenshteinDistance(normalizedClean, normalizedDictWord, maxAllowedDistance)

            if (distance in 1..maxAllowedDistance) {
                val matchesFirst = normalizedDictWord.isNotEmpty() && normalizedClean.isNotEmpty() && normalizedDictWord.first() == normalizedClean.first()
                if (distance < minDistance) {
                    minDistance = distance
                    bestCandidate = word
                    bestCandidateMatchesFirstChar = matchesFirst
                } else if (distance == minDistance) {
                    // Prefer candidate matching first character
                    if (!bestCandidateMatchesFirstChar && matchesFirst) {
                        bestCandidate = word
                        bestCandidateMatchesFirstChar = true
                    }
                }
            }
        }

        return bestCandidate?.let { formatCapitalization(it, clean) }
    }

    private fun formatCapitalization(targetWord: String, originalInput: String): String {
        return if (originalInput.isNotEmpty() && originalInput[0].isUpperCase()) {
            if (originalInput.all { it.isUpperCase() } && originalInput.length > 1) {
                targetWord.uppercase()
            } else {
                targetWord.replaceFirstChar { it.uppercase() }
            }
        } else {
            targetWord
        }
    }

    /**
     * Automatic language detection based on the last 2-3 typed words.
     * Returns KeyboardLanguage.PT_BR, KeyboardLanguage.EN_US, or null if confidence is inconclusive.
     */
    fun detectLanguageFromWords(words: List<String>): KeyboardLanguage? {
        val cleanWords = words.map { normalize(it.trim().lowercase()) }.filter { it.length >= 2 }
        if (cleanWords.isEmpty()) return null

        var ptScore = 0
        var enScore = 0

        for (word in cleanWords) {
            // Accent detection in original form
            if (portugueseExclusiveWords.contains(word)) {
                ptScore += 2
            } else if (portugueseNormalizedSet.contains(word)) {
                ptScore += 1
            }

            if (englishExclusiveWords.contains(word)) {
                enScore += 2
            } else if (englishNormalizedSet.contains(word)) {
                enScore += 1
            }
        }

        // Strong confidence threshold
        if (ptScore >= 2 && enScore == 0) {
            return KeyboardLanguage.PT_BR
        }
        if (enScore >= 2 && ptScore == 0) {
            return KeyboardLanguage.EN_US
        }
        if (ptScore >= 4 && ptScore > enScore * 2) {
            return KeyboardLanguage.PT_BR
        }
        if (enScore >= 4 && enScore > ptScore * 2) {
            return KeyboardLanguage.EN_US
        }

        return null
    }

    fun getSuggestions(prefix: String, language: KeyboardLanguage, limit: Int = 3): List<String> {
        val rawClean = prefix.trim()
        if (rawClean.isEmpty()) {
            val defaults = if (language == KeyboardLanguage.PT_BR) {
                listOf("Olá", "Você", "Como")
            } else {
                listOf("Hello", "How", "Thanks")
            }
            return (userCustomWords.take(1) + defaults).distinct().take(limit)
        }

        val cleanPrefix = rawClean.lowercase()
        val normalizedPrefix = normalize(cleanPrefix)

        val wordList = when (language) {
            KeyboardLanguage.PT_BR -> {
                if (normalizedPrefix.length >= 2 && ptPrefixIndex.isNotEmpty()) {
                    val key = normalizedPrefix.substring(0, 2)
                    ptPrefixIndex[key] ?: emptyList()
                } else {
                    portugueseWords
                }
            }
            KeyboardLanguage.EN_US -> {
                if (normalizedPrefix.length >= 2 && enPrefixIndex.isNotEmpty()) {
                    val key = normalizedPrefix.substring(0, 2)
                    enPrefixIndex[key] ?: emptyList()
                } else {
                    englishWords
                }
            }
        }

        val customMatches = mutableListOf<String>()
        for (custom in userCustomWords) {
            val lowerCustom = custom.lowercase()
            val normCustom = normalize(lowerCustom)
            if (lowerCustom == cleanPrefix || normCustom == normalizedPrefix || lowerCustom.startsWith(cleanPrefix) || normCustom.startsWith(normalizedPrefix)) {
                customMatches.add(custom)
            }
        }

        val exactMatches = mutableListOf<String>()
        val exactNormalizedMatches = mutableListOf<String>()
        val prefixMatches = mutableListOf<String>()
        val prefixNormalizedMatches = mutableListOf<String>()

        for (word in wordList) {
            val lowerWord = word.lowercase()
            val normalizedWord = normalize(lowerWord)

            if (lowerWord == cleanPrefix) {
                exactMatches.add(word)
            } else if (normalizedWord == normalizedPrefix) {
                exactNormalizedMatches.add(word)
            } else if (lowerWord.startsWith(cleanPrefix)) {
                prefixMatches.add(word)
            } else if (normalizedWord.startsWith(normalizedPrefix)) {
                prefixNormalizedMatches.add(word)
            }

            if (exactMatches.size + exactNormalizedMatches.size + prefixMatches.size >= limit * 3) {
                break
            }
        }

        val results = mutableListOf<String>()
        results.addAll(customMatches)
        results.addAll(exactMatches)
        results.addAll(exactNormalizedMatches)
        results.addAll(prefixMatches)
        results.addAll(prefixNormalizedMatches)

        val isCapitalized = rawClean.isNotEmpty() && rawClean[0].isUpperCase()
        val formatted = results.distinct().take(limit).map { word ->
            if (isCapitalized && word.isNotEmpty()) {
                word.replaceFirstChar { it.uppercase() }
            } else {
                word
            }
        }

        return if (formatted.isNotEmpty()) {
            formatted
        } else {
            listOf(rawClean)
        }
    }
}

