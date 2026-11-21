package com.example.stickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import java.io.File
import java.io.FileOutputStream

data class StickerItem(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String,
    val bgColorHex: Long,
    val textColorHex: Long = 0xFFFFFFFF
)

object StickerData {
    val categories = listOf("Destaques", "Expressões", "Trabalho & Dev", "Social")

    val stickers = listOf(
        // Destaques
        StickerItem("stk_valeu", "Valeu!", "🙌", "Destaques", 0xFF6366F1),
        StickerItem("stk_top", "Top Demais!", "🚀", "Destaques", 0xFF10B981),
        StickerItem("stk_bora_codar", "Bora Codar!", "💻", "Destaques", 0xFF8B5CF6),
        StickerItem("stk_cafezin", "Hora do Café", "☕", "Destaques", 0xFFB45309),
        StickerItem("stk_show", "Show de Bola!", "🎉", "Destaques", 0xFFEC4899),
        StickerItem("stk_foco", "Foco Total!", "🔥", "Destaques", 0xFFEF4444),

        // Expressões
        StickerItem("stk_tmj", "Tamo Junto!", "🤝", "Expressões", 0xFF0EA5E9),
        StickerItem("stk_parabens", "Parabéns!", "🎂", "Expressões", 0xFFF59E0B),
        StickerItem("stk_deu_bom", "Deu Bom!", "😎", "Expressões", 0xFF10B981),
        StickerItem("stk_offline", "100% Offline", "⚡", "Expressões", 0xFF6366F1),
        StickerItem("stk_sensacional", "Sensacional!", "✨", "Expressões", 0xFFF43F5E),
        StickerItem("stk_tranquilo", "Tudo Certo!", "👌", "Expressões", 0xFF14B8A6),

        // Trabalho & Dev
        StickerItem("stk_aprovado", "LGTM / Aprovado", "✅", "Trabalho & Dev", 0xFF10B981),
        StickerItem("stk_pr_review", "Pull Request Aberto", "🔀", "Trabalho & Dev", 0xFF6366F1),
        StickerItem("stk_deploy_ok", "Deploy no Ar!", "🚢", "Trabalho & Dev", 0xFF3B82F6),
        StickerItem("stk_bug_fix", "Bug Corrigido", "🐞", "Trabalho & Dev", 0xFFEF4444),
        StickerItem("stk_reuniao", "Partiu Reunião", "📅", "Trabalho & Dev", 0xFF8B5CF6),
        StickerItem("stk_sextou_dev", "Sextou sem deploy!", "🍻", "Trabalho & Dev", 0xFFF59E0B),

        // Social
        StickerItem("stk_bom_dia", "Bom Dia!", "☀️", "Social", 0xFFF59E0B),
        StickerItem("stk_boa_noite", "Boa Noite!", "🌙", "Social", 0xFF475569),
        StickerItem("stk_partiu", "Partiu!", "🛵", "Social", 0xFF06B6D4),
        StickerItem("stk_abraco", "Aquele Abraço!", "🤗", "Social", 0xFFEC4899),
        StickerItem("stk_gratidao", "Gratidão!", "🙏", "Social", 0xFF8B5CF6),
        StickerItem("stk_sucesso", "Muito Sucesso!", "🏆", "Social", 0xFFEAB308)
    )
}

object StickerHelper {

    /**
     * Creates a high resolution PNG image file for the given sticker in cache.
     */
    fun createStickerBitmapFile(context: Context, sticker: StickerItem): File {
        val dir = File(context.cacheDir, "stickers").apply { mkdirs() }
        val file = File(dir, "${sticker.id}.png")

        if (file.exists() && file.length() > 0) {
            return file
        }

        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background rounded card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sticker.bgColorHex.toInt()
            style = Paint.Style.FILL
        }
        val rect = RectF(16f, 16f, size - 16f, size - 16f)
        canvas.drawRoundRect(rect, 48f, 48f, bgPaint)

        // Subtle inner border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 70
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(rect, 48f, 48f, borderPaint)

        // Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 150f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(sticker.emoji, size / 2f, size / 2f - 10f, emojiPaint)

        // Title text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sticker.textColorHex.toInt()
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(sticker.title, size / 2f, size / 2f + 120f, textPaint)

        // Badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 60
        }
        val badgeRect = RectF(size / 2f - 100f, size - 70f, size / 2f + 100f, size - 36f)
        canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FlexiKey", size / 2f, size - 46f, badgeTextPaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file
    }

    /**
     * Attempts to commit rich content sticker into the active input connection.
     * If the target app doesn't accept image/png, fallbacks to copying to clipboard.
     */
    fun sendSticker(
        context: Context,
        sticker: StickerItem,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        onFeedback: (String) -> Unit
    ) {
        try {
            val file = createStickerBitmapFile(context, sticker)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            var committed = false

            if (inputConnection != null && editorInfo != null) {
                val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
                val isImageAccepted = mimeTypes.any { mime ->
                    ClipDescription.compareMimeTypes(mime, "image/png") ||
                        ClipDescription.compareMimeTypes(mime, "image/*")
                }

                if (isImageAccepted) {
                    val inputContentInfo = InputContentInfoCompat(
                        uri,
                        ClipDescription(sticker.title, arrayOf("image/png")),
                        null
                    )

                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                    } else {
                        0
                    }

                    committed = InputConnectionCompat.commitContent(
                        inputConnection,
                        editorInfo,
                        inputContentInfo,
                        flags,
                        null
                    )
                }
            }

            if (committed) {
                onFeedback("Figurinha enviada!")
            } else {
                // Fallback: copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    val clip = ClipData.newUri(context.contentResolver, sticker.title, uri)
                    clipboard.setPrimaryClip(clip)
                    onFeedback("App não aceita imagem direta. Figurinha copiada para a área de transferência!")
                } else {
                    onFeedback("Não foi possível enviar a figurinha neste campo.")
                }
            }
        } catch (e: Exception) {
            onFeedback("Erro ao processar figurinha: ${e.localizedMessage}")
        }
    }
}
