package com.example.keyboard.data

import android.content.Context
import com.example.keyboard.data.db.AppPhraseDao
import com.example.keyboard.data.db.AppPhraseEntity
import com.example.keyboard.data.db.KeyboardDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class InferredTone(val label: String, val emoji: String) {
    FORMAL("Formal / Profissional", "👔"),
    CASUAL("Casual / Descontraído", "💬"),
    NEUTRAL("Neutro", "⚡")
}

class AppPhrasesRepository private constructor(context: Context) {

    private val db = KeyboardDatabase.getInstance(context)
    private val dao: AppPhraseDao = db.appPhraseDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    val allPhrasesFlow: Flow<List<AppPhraseEntity>> = dao.getAllPhrasesFlow()

    init {
        scope.launch {
            if (dao.getCount() == 0) {
                dao.insertAll(DEFAULT_PHRASES)
            }
        }
    }

    fun getPhrasesForPackageFlow(packageName: String?): Flow<List<String>> {
        val category = getCategoryForPackage(packageName)
        return dao.getPhrasesByCategoryFlow(category).map { list ->
            list.map { it.phrase }
        }
    }

    fun getPhrasesByCategoryFlow(category: String): Flow<List<AppPhraseEntity>> {
        return dao.getPhrasesByCategoryFlow(category)
    }

    fun addPhrase(category: String, phrase: String) {
        val clean = phrase.trim()
        if (clean.isEmpty()) return
        scope.launch {
            dao.insert(
                AppPhraseEntity(
                    appCategory = category,
                    phrase = clean,
                    isCustom = true
                )
            )
        }
    }

    fun deletePhrase(id: Long) {
        scope.launch {
            dao.deleteById(id)
        }
    }

    fun resetToDefaults() {
        scope.launch {
            dao.clearAll()
            dao.insertAll(DEFAULT_PHRASES)
        }
    }

    companion object {
        const val CATEGORY_MESSAGING = "messaging"
        const val CATEGORY_EMAIL = "email"
        const val CATEGORY_GENERAL = "general"

        val MESSAGING_PACKAGES = setOf(
            "whatsapp", "telegram", "facebook.orca", "instagram",
            "discord", "twitter", "slack", "viber", "signal", "skype"
        )

        val EMAIL_PACKAGES = setOf(
            "gm", "gmail", "outlook", "mail", "protonmail", "email", "notes", "docs"
        )

        fun getCategoryForPackage(packageName: String?): String {
            if (packageName == null) return CATEGORY_GENERAL
            val pkg = packageName.lowercase()

            return when {
                MESSAGING_PACKAGES.any { pkg.contains(it) } -> CATEGORY_MESSAGING
                EMAIL_PACKAGES.any { pkg.contains(it) } -> CATEGORY_EMAIL
                else -> CATEGORY_GENERAL
            }
        }

        fun getCategoryLabel(category: String): String {
            return when (category) {
                CATEGORY_MESSAGING -> "Mensagens & Chat (WhatsApp, etc.)"
                CATEGORY_EMAIL -> "E-mails & Formal (Gmail, Outlook)"
                else -> "Geral & Padrão"
            }
        }

        val DEFAULT_PHRASES = listOf(
            // Messaging Phrases
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Opa, tudo bem?"),
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Já estou a caminho!"),
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Fechado! Combinado."),
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Pode falar agora?"),
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Depois te ligo!"),
            AppPhraseEntity(appCategory = CATEGORY_MESSAGING, phrase = "Valeu, abraço!"),

            // Email & Formal Phrases
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Prezados,"),
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Segue em anexo conforme solicitado."),
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Fico no aguardo do seu retorno."),
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Qualquer dúvida, estou à disposição."),
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Atenciosamente,"),
            AppPhraseEntity(appCategory = CATEGORY_EMAIL, phrase = "Obrigado pela atenção e suporte."),

            // General & Default Phrases
            AppPhraseEntity(appCategory = CATEGORY_GENERAL, phrase = "OK, combinado!"),
            AppPhraseEntity(appCategory = CATEGORY_GENERAL, phrase = "Muito obrigado!"),
            AppPhraseEntity(appCategory = CATEGORY_GENERAL, phrase = "Perfeito, agradeço!"),
            AppPhraseEntity(appCategory = CATEGORY_GENERAL, phrase = "Tudo certo por aqui."),
            AppPhraseEntity(appCategory = CATEGORY_GENERAL, phrase = "Excelente, combinado!")
        )

        private val FORMAL_KEYWORDS = setOf(
            "prezado", "prezados", "prezada", "atenciosamente", "cordialmente", "solicito",
            "gentileza", "agradeco", "agradeço", "aguardo", "reuniao", "reunião", "relatorio",
            "relatório", "anexo", "conforme", "disposicao", "disposição", "senhor", "senhora",
            "dear", "sincerely", "regards", "attached", "please", "meeting", "report"
        )

        private val CASUAL_KEYWORDS = setOf(
            "opa", "eai", "e aí", "e ai", "fala", "mano", "bora", "valeu", "show", "beleza",
            "blz", "vc", "tb", "tbm", "tmj", "kkk", "haha", "rsrs", "top", "tranquilo",
            "massa", "brother", "cara", "hey", "hi", "lol", "cool", "yo", "sup"
        )

        fun inferTone(recentText: String): InferredTone {
            if (recentText.isBlank()) return InferredTone.NEUTRAL
            val lower = recentText.lowercase()
            val tokens = lower.split(Regex("[\\s,;.!?:()\\[\\]\"]+")).filter { it.isNotBlank() }

            var formalScore = 0
            var casualScore = 0

            for (token in tokens) {
                if (FORMAL_KEYWORDS.contains(token)) formalScore += 2
                if (CASUAL_KEYWORDS.contains(token)) casualScore += 2
            }

            return when {
                formalScore > casualScore && formalScore >= 2 -> InferredTone.FORMAL
                casualScore > formalScore && casualScore >= 2 -> InferredTone.CASUAL
                else -> InferredTone.NEUTRAL
            }
        }

        fun getToneSortedPhrases(phrases: List<String>, tone: InferredTone): List<String> {
            return when (tone) {
                InferredTone.FORMAL -> {
                    phrases.sortedByDescending { phrase ->
                        val lower = phrase.lowercase()
                        if (FORMAL_KEYWORDS.any { lower.contains(it) }) 2 else if (CASUAL_KEYWORDS.any { lower.contains(it) }) 0 else 1
                    }
                }
                InferredTone.CASUAL -> {
                    phrases.sortedByDescending { phrase ->
                        val lower = phrase.lowercase()
                        if (CASUAL_KEYWORDS.any { lower.contains(it) }) 2 else if (FORMAL_KEYWORDS.any { lower.contains(it) }) 0 else 1
                    }
                }
                InferredTone.NEUTRAL -> phrases
            }
        }

        @Volatile
        private var instance: AppPhrasesRepository? = null

        fun getInstance(context: Context): AppPhrasesRepository {
            return instance ?: synchronized(this) {
                instance ?: AppPhrasesRepository(context).also { instance = it }
            }
        }
    }
}
