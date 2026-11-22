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
    private var loadedPortugueseAccentMap: Map<String, List<String>> = emptyMap()
    @Volatile
    private var loadedEnglishAccentMap: Map<String, List<String>> = emptyMap()

    // Prefix index: map from 2-letter normalized prefix to list of words for O(1) prefix lookup
    @Volatile
    private var ptPrefixIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var enPrefixIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var ptOneCharIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var enOneCharIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var ptNormalizedByWord: Map<String, String> = emptyMap()
    @Volatile
    private var enNormalizedByWord: Map<String, String> = emptyMap()
    @Volatile
    private var ptDeletionIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var enDeletionIndex: Map<String, List<String>> = emptyMap()
    @Volatile
    private var ptWordsByNormalized: Map<String, List<String>> = emptyMap()
    @Volatile
    private var enWordsByNormalized: Map<String, List<String>> = emptyMap()

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

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
            val ptAccentMap = HashMap<String, MutableList<String>>(15000)
            val ptIndex = HashMap<String, MutableList<String>>()
            val ptOneIndex = HashMap<String, MutableList<String>>()
            val ptNormalized = HashMap<String, String>(35000)
            val ptDeletes = HashMap<String, MutableSet<String>>()
            val ptByNormalized = HashMap<String, MutableSet<String>>()

            try {
                loadLanguageAsset(context, "dict_pt_br.txt", ptWords, ptNormSet, ptExactSet, ptAccentMap, ptIndex, ptOneIndex, ptNormalized, ptDeletes, ptByNormalized)
                loadedPortugueseWords = ptWords
                loadedPortugueseNormalizedSet = ptNormSet
                loadedPortugueseExactSet = ptExactSet
                loadedPortugueseAccentMap = ptAccentMap.mapValues { it.value.distinct() }
                ptPrefixIndex = ptIndex.mapValues { it.value.toList() }
                ptOneCharIndex = ptOneIndex.mapValues { it.value.toList() }
                ptNormalizedByWord = ptNormalized
                ptDeletionIndex = ptDeletes.mapValues { it.value.toList() }
                ptWordsByNormalized = ptByNormalized.mapValues { it.value.toList() }
            } catch (_: Exception) {}

            val enWords = ArrayList<String>(25000)
            val enNormSet = HashSet<String>(25000)
            val enExactSet = HashSet<String>(25000)
            val enAccentMap = HashMap<String, MutableList<String>>(5000)
            val enIndex = HashMap<String, MutableList<String>>()
            val enOneIndex = HashMap<String, MutableList<String>>()
            val enNormalized = HashMap<String, String>(25000)
            val enDeletes = HashMap<String, MutableSet<String>>()
            val enByNormalized = HashMap<String, MutableSet<String>>()

            try {
                loadLanguageAsset(context, "dict_en_us.txt", enWords, enNormSet, enExactSet, enAccentMap, enIndex, enOneIndex, enNormalized, enDeletes, enByNormalized)
                loadedEnglishWords = enWords
                loadedEnglishNormalizedSet = enNormSet
                loadedEnglishExactSet = enExactSet
                loadedEnglishAccentMap = enAccentMap.mapValues { it.value.distinct() }
                enPrefixIndex = enIndex.mapValues { it.value.toList() }
                enOneCharIndex = enOneIndex.mapValues { it.value.toList() }
                enNormalizedByWord = enNormalized
                enDeletionIndex = enDeletes.mapValues { it.value.toList() }
                enWordsByNormalized = enByNormalized.mapValues { it.value.toList() }
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
        targetAccentMap: MutableMap<String, MutableList<String>>,
        prefixIndex: MutableMap<String, MutableList<String>>,
        oneCharIndex: MutableMap<String, MutableList<String>>,
        normalizedByWord: MutableMap<String, String>,
        deletionIndex: MutableMap<String, MutableSet<String>>,
        wordsByNormalized: MutableMap<String, MutableSet<String>>
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
                    if (norm != lower) targetAccentMap.getOrPut(norm) { mutableListOf() }.add(lower)
                    normalizedByWord[lower] = norm
                    wordsByNormalized.getOrPut(norm) { linkedSetOf() }.add(lower)
                    for (index in norm.indices) {
                        val deletion = norm.removeRange(index, index + 1)
                        deletionIndex.getOrPut(deletion) { linkedSetOf() }.add(lower)
                    }
                    if (norm.isNotEmpty()) oneCharIndex.getOrPut(norm.substring(0, 1)) { mutableListOf() }.add(lower)
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
        return temp.replace(combiningMarksRegex, "").lowercase()
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
        "abraços", "beleza", "legal", "tranquilo", "show", "maravilha", "perfeito", "ótimo", "ótima", "avó", "avô",
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

    @Synchronized
    fun addUserCustomWord(word: String) {
        val clean = word.trim()
        if (clean.isBlank()) return
        val normalized = normalize(clean)
        if (normalized !in userCustomNormalizedSet) {
            userCustomWords.add(clean)
            userCustomNormalizedSet.add(normalized)
        }
    }

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
        val customWords = getUserCustomWords()

        // 0. If it's a learned / user custom word (e.g. "Kubernetes", "Kotlin", "microserviços"), it's valid!
        if (customWords.any { it.equals(clean, ignoreCase = true) } || userCustomNormalizedSet.contains(normalizedClean)) {
            return null
        }

        // Restore a unique accent variant ("voce" -> "você", "nao" -> "não").
        // If multiple accented words share the same unaccented form ("avo" -> "avó"/"avô"), do not guess.
        if (isWordInDictionary(clean, language)) {
            val accentlessInput = lowerClean == normalizedClean
            val accentVariants = accentVariants(normalizedClean, language)
            val restored = if (accentlessInput) accentVariants.singleOrNull() else null
            return restored?.let { formatCapitalization(it, clean) }
        }

        // Only inspect nearby word lengths and only auto-correct an unambiguous one-edit match.
        // Ambiguous alternatives remain available as suggestions instead of silently replacing text.
        val candidates = nearbyWordCandidates(normalizedClean, language)
        val matches = mutableListOf<String>()
        for (word in customWords.filterNot { it.contains(' ') } + candidates) {
            val normalizedWord = cachedNormalize(word, language)
            if (kotlin.math.abs(normalizedWord.length - normalizedClean.length) > 1) continue
            if (levenshteinDistance(normalizedClean, normalizedWord, maxAllowed = 1) == 1) {
                matches.add(word)
            }
        }

        val preferredMatches = matches.distinct().let { all ->
            val sameInitial = all.filter { cachedNormalize(it, language).firstOrNull() == normalizedClean.firstOrNull() }
            if (sameInitial.isNotEmpty()) sameInitial else all
        }
        return preferredMatches.singleOrNull()?.let { formatCapitalization(it, clean) }
    }

    private fun cachedNormalize(word: String, language: KeyboardLanguage): String {
        val cache = if (language == KeyboardLanguage.PT_BR) ptNormalizedByWord else enNormalizedByWord
        return cache[word.lowercase()] ?: normalize(word)
    }

    private fun nearbyWordCandidates(normalizedInput: String, language: KeyboardLanguage): List<String> {
        val wordList = if (language == KeyboardLanguage.PT_BR) portugueseWords else englishWords
        val deletionIndex = if (language == KeyboardLanguage.PT_BR) ptDeletionIndex else enDeletionIndex
        if (deletionIndex.isEmpty()) return wordList

        val wordsByNormalized = if (language == KeyboardLanguage.PT_BR) ptWordsByNormalized else enWordsByNormalized
        val candidates = LinkedHashSet<String>()
        deletionIndex[normalizedInput]?.let(candidates::addAll)
        for (index in normalizedInput.indices) {
            val shortened = normalizedInput.removeRange(index, index + 1)
            deletionIndex[shortened]?.let(candidates::addAll)
            wordsByNormalized[shortened]?.let(candidates::addAll)
        }
        return candidates.toList()
    }

    private fun accentVariants(normalizedWord: String, language: KeyboardLanguage): List<String> {
        val map = if (language == KeyboardLanguage.PT_BR) loadedPortugueseAccentMap else loadedEnglishAccentMap
        val fallback = if (language == KeyboardLanguage.PT_BR) fallbackPortugueseWords else fallbackEnglishWords
        return (map[normalizedWord].orEmpty() + fallback.filter { normalize(it) == normalizedWord && it.lowercase() != normalizedWord })
            .distinct()
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

    private val portugueseNextWords = mapOf(
        "bom" to listOf("dia", "trabalho", "apetite"),
        "boa" to listOf("tarde", "noite", "sorte"),
        "como" to listOf("você", "vai", "está"),
        "você" to listOf("está", "pode", "quer"),
        "eu" to listOf("vou", "quero", "estou", "não", "gosto"),
        "não" to listOf("sei", "consigo", "quero", "posso", "tem"),
        "muito" to listOf("obrigado", "obrigada", "bem", "bom"),
        "preciso" to listOf("de", "falar", "saber"),
        "gosto" to listOf("de", "muito", "disso"),
        "tem" to listOf("que", "como", "mais"),
        "por" to listOf("favor", "isso", "que"),
        "até" to listOf("logo", "amanhã", "mais"),
        "valeu" to listOf("mesmo", "pela", "por"),
        "está" to listOf("bem", "tudo", "certo"),
        "obrigado" to listOf("por", "pela", "mesmo")
    )

    private val englishNextWords = mapOf(
        "good" to listOf("morning", "day", "job"),
        "how" to listOf("are", "do", "about"),
        "i" to listOf("am", "will", "want", "can", "have"),
        "you" to listOf("are", "can", "have", "will"),
        "thank" to listOf("you", "you!"),
        "please" to listOf("let", "send", "help"),
        "have" to listOf("a", "been", "to"),
        "would" to listOf("like", "you", "be"),
        "looking" to listOf("for", "forward", "good"),
        "see" to listOf("you", "it", "the"),
        "talk" to listOf("to", "soon", "about")
    )

    fun getSuggestions(
        prefix: String,
        language: KeyboardLanguage,
        limit: Int = 3,
        previousWord: String? = null
    ): List<String> {
        val rawClean = prefix.trim()
        val customWords = getUserCustomWords()
        val nextWordsMap = if (language == KeyboardLanguage.PT_BR) portugueseNextWords else englishNextWords
        val nextWords = nextWordsMap[normalize(previousWord.orEmpty())].orEmpty()
        val contextualMatches = nextWords.filter { candidate ->
            rawClean.isEmpty() || normalize(candidate).startsWith(normalize(rawClean))
        }
        if (rawClean.isEmpty()) {
            val defaults = if (language == KeyboardLanguage.PT_BR) {
                listOf("Olá", "Você", "Como")
            } else {
                listOf("Hello", "How", "Thanks")
            }
            return (contextualMatches + customWords.take(1) + defaults).distinct().take(limit)
        }

        val cleanPrefix = rawClean.lowercase()
        val normalizedPrefix = normalize(cleanPrefix)
        val wordList = if (language == KeyboardLanguage.PT_BR) portugueseWords else englishWords
        val prefixIndex = if (language == KeyboardLanguage.PT_BR) ptPrefixIndex else enPrefixIndex
        val oneCharIndex = if (language == KeyboardLanguage.PT_BR) ptOneCharIndex else enOneCharIndex
        val normalizedMap = if (language == KeyboardLanguage.PT_BR) ptNormalizedByWord else enNormalizedByWord
        val candidates = when {
            normalizedPrefix.length >= 2 && prefixIndex.isNotEmpty() -> prefixIndex[normalizedPrefix.substring(0, 2)].orEmpty()
            normalizedPrefix.length == 1 && oneCharIndex.isNotEmpty() -> oneCharIndex[normalizedPrefix].orEmpty()
            else -> wordList
        }
        val customMatches = mutableListOf<String>()
        for (custom in customWords) {
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

        for (word in candidates) {
            val lowerWord = word.lowercase()
            val normalizedWord = normalizedMap[lowerWord] ?: normalize(lowerWord)

            if (lowerWord == cleanPrefix) {
                exactMatches.add(word)
            } else if (normalizedWord == normalizedPrefix) {
                exactNormalizedMatches.add(word)
            } else if (lowerWord.startsWith(cleanPrefix)) {
                prefixMatches.add(word)
            } else if (normalizedWord.startsWith(normalizedPrefix)) {
                prefixNormalizedMatches.add(word)
            }

            if (exactMatches.size + exactNormalizedMatches.size + prefixMatches.size + prefixNormalizedMatches.size >= limit * 4) {
                break
            }
        }

        val results = mutableListOf<String>()
        results.addAll(customMatches)
        if (cleanPrefix == normalizedPrefix) {
            results.addAll(exactNormalizedMatches)
            results.addAll(prefixNormalizedMatches)
            results.addAll(exactMatches)
            results.addAll(prefixMatches)
        } else {
            results.addAll(exactMatches)
            results.addAll(prefixMatches)
            results.addAll(exactNormalizedMatches)
            results.addAll(prefixNormalizedMatches)
        }

        results.addAll(0.coerceAtMost(results.size), contextualMatches)

        if (results.isEmpty() && normalizedPrefix.length >= 3) {
            val nearWords = nearbyWordCandidates(normalizedPrefix, language)
            for (word in nearWords) {
                val normalizedWord = normalizedMap[word.lowercase()] ?: normalize(word)
                if (levenshteinDistance(normalizedPrefix, normalizedWord, maxAllowed = 1) == 1) {
                    results.add(word)
                    if (results.size >= limit) break
                }
            }
        }

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
