package com.example.stickers

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object GiphyGifHelper {
    private const val MAX_DOWNLOAD_BYTES = 20L * 1024L * 1024L
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun send(
        context: Context,
        item: GiphyMediaItem,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?
    ): String {
        val appContext = context.applicationContext
        val acceptsImage = inputConnection != null && editorInfo != null &&
            EditorInfoCompat.getContentMimeTypes(editorInfo).any { mime ->
                ClipDescription.compareMimeTypes(mime, "image/gif") ||
                    ClipDescription.compareMimeTypes(mime, "image/*")
            }

        if (!acceptsImage) {
            return withContext(Dispatchers.Main) { copyLink(appContext, item.mediaUrl) }
        }

        val file = withContext(Dispatchers.IO) { downloadGif(appContext, item) }
        return withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
            val contentInfo = InputContentInfoCompat(
                uri,
                ClipDescription(item.title, arrayOf("image/gif")),
                null
            )
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            } else 0
            val committed = InputConnectionCompat.commitContent(
                inputConnection!!,
                editorInfo!!,
                contentInfo,
                flags,
                null
            )
            if (committed) "GIF enviado!" else copyLink(appContext, item.mediaUrl)
        }
    }

    private fun downloadGif(context: Context, item: GiphyMediaItem): File {
        val mediaUri = Uri.parse(item.mediaUrl)
        val host = mediaUri.host.orEmpty().lowercase()
        require(mediaUri.scheme == "https" && (host == "giphy.com" || host.endsWith(".giphy.com"))) {
            "Endereço de mídia não confiável."
        }
        val request = Request.Builder().url(item.mediaUrl).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Não foi possível baixar o GIF.")
            val body = response.body ?: throw IOException("A resposta do GIF está vazia.")
            if (body.contentLength() > MAX_DOWNLOAD_BYTES) throw IOException("O GIF é grande demais para enviar.")
            val folder = File(context.cacheDir, "giphy").apply { mkdirs() }
            val safeId = item.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "media" }
            val file = File(folder, "$safeId.gif")
            body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            if (file.length() > MAX_DOWNLOAD_BYTES) {
                file.delete()
                throw IOException("O GIF é grande demais para enviar.")
            }
            return file
        }
    }

    private fun copyLink(context: Context, url: String): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return "Este app não aceita GIF direto e a área de transferência não está disponível."
        clipboard.setPrimaryClip(ClipData.newPlainText("GIF do GIPHY", url))
        return "Este app não aceita GIF direto. Link do GIF copiado."
    }
}
