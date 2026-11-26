package com.aegis.survival.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@Serializable
private data class ProxyHistoryItem(
    val role: String,
    val content: String,
)

@Serializable
private data class ProxyChatRequest(
    val message: String,
    val emergency: Boolean,
    val domain: String,
    val history: List<ProxyHistoryItem>,
)

@Serializable
data class ProxyChatResponse(
    val reply: String = "",
    val source: String = "server-proxy",
    val error: String? = null,
)

class OnlineProxyClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun isValidHttpsBaseUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendChat(
        baseUrl: String,
        message: String,
        emergency: Boolean,
        domain: String,
        recentMessages: List<ChatMessage>,
    ): Result<ProxyChatResponse> = withContext(Dispatchers.IO) {
        if (!isValidHttpsBaseUrl(baseUrl)) {
            return@withContext Result.failure(IllegalArgumentException("Proxy URL must use https://"))
        }

        val endpoint = baseUrl.trim().trimEnd('/') + "/api/ai/chat"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val history = recentMessages
                .filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT || it.role == ChatRole.EMERGENCY }
                .takeLast(6)
                .map {
                    ProxyHistoryItem(
                        role = if (it.role == ChatRole.USER) "user" else "assistant",
                        content = it.text.take(1000),
                    )
                }

            val payload = json.encodeToString(
                ProxyChatRequest(
                    message = message.trim().take(2000),
                    emergency = emergency,
                    domain = domain.ifBlank { "all" },
                    history = history,
                )
            )

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()

            if (status in 200..299 && body.isNotBlank()) {
                val parsed = json.decodeFromString<ProxyChatResponse>(body)
                if (parsed.reply.isNotBlank()) {
                    Result.success(parsed)
                } else {
                    Result.failure(IllegalStateException("Empty reply from server proxy"))
                }
            } else {
                val errMsg = try {
                    json.decodeFromString<ProxyChatResponse>(body).error ?: "HTTP $status"
                } catch (_: Exception) {
                    "HTTP $status"
                }
                Result.failure(IllegalStateException(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
