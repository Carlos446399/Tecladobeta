package com.example.stickers

import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GiphyMediaItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val mediaUrl: String,
    val isSticker: Boolean
)

object GiphyMediaRepository {
    private const val API_HOST = "api.giphy.com"
    private const val MAX_RESULTS = 24
    private const val MAX_QUERY_LENGTH = 50

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = BuildConfig.GIPHY_API_KEY.isNotBlank()

    suspend fun search(query: String, isSticker: Boolean): List<GiphyMediaItem> {
        val cleanedQuery = query.trim().take(MAX_QUERY_LENGTH)
        require(cleanedQuery.isNotEmpty()) { "Digite um termo para pesquisar." }
        return requestMedia(cleanedQuery, isSticker, trending = false)
    }

    suspend fun trending(isSticker: Boolean): List<GiphyMediaItem> =
        requestMedia(query = null, isSticker = isSticker, trending = true)

    private suspend fun requestMedia(
        query: String?,
        isSticker: Boolean,
        trending: Boolean
    ): List<GiphyMediaItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GIPHY_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("A chave GIPHY_API_KEY não está configurada.")

        val endpoint = when {
            isSticker && trending -> "v1/stickers/trending"
            isSticker -> "v1/stickers/search"
            trending -> "v1/gifs/trending"
            else -> "v1/gifs/search"
        }
        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host(API_HOST)
            .addPathSegments(endpoint)
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("limit", MAX_RESULTS.toString())
            .addQueryParameter("rating", "g")
            .addQueryParameter("lang", "pt")
        if (!query.isNullOrBlank()) urlBuilder.addQueryParameter("q", query)

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GIPHY respondeu com erro ${response.code}.")
            }
            val payload = response.body?.string().orEmpty()
            val data = JSONObject(payload).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val images = item.optJSONObject("images") ?: continue
                    val previewUrl = firstImageUrl(
                        images.optJSONObject("fixed_width_small_still"),
                        images.optJSONObject("fixed_width_still"),
                        images.optJSONObject("downsized_still"),
                        images.optJSONObject("original_still"),
                        images.optJSONObject("fixed_width")
                    ) ?: continue
                    val mediaUrl = images.optJSONObject("original")?.optString("url")
                        ?.takeIf(String::isNotBlank)
                        ?: images.optJSONObject("fixed_width")?.optString("url")?.takeIf(String::isNotBlank)
                        ?: continue
                    if (!isTrustedMediaUrl(previewUrl) || !isTrustedMediaUrl(mediaUrl)) continue
                    add(
                        GiphyMediaItem(
                            id = item.optString("id", "giphy_$index"),
                            title = item.optString("title").ifBlank { if (isSticker) "Figurinha" else "GIF" },
                            previewUrl = previewUrl,
                            mediaUrl = mediaUrl,
                            isSticker = isSticker
                        )
                    )
                }
            }
        }
    }

    private fun firstImageUrl(vararg sources: JSONObject?): String? =
        sources.firstNotNullOfOrNull { it?.optString("url")?.takeIf(String::isNotBlank) }

    private fun isTrustedMediaUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        val host = uri.host.orEmpty().lowercase()
        return uri.scheme == "https" && (host == "giphy.com" || host.endsWith(".giphy.com"))
    }
}
