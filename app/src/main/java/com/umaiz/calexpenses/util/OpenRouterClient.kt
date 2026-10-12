package com.umaiz.calexpenses.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenRouter client. Talks to the OpenAI-compatible chat completions
 * endpoint using the built-in HttpURLConnection (no extra HTTP dependency).
 *
 * The API key is passed in at call time and is never logged or persisted here.
 */
class OpenRouterClient(private val apiKey: String) {

    suspend fun chat(history: List<ChatMessage>, model: String = DEFAULT_MODEL): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://calexpenses.app")
                setRequestProperty("X-Title", "Cal-Expenses")
            }

            val messages = JSONArray()
            history.forEach { m ->
                messages.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("max_tokens", 800)
                put("temperature", 0.7)
            }

            try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) throw IOException(describeError(code, body))

                val content = JSONObject(body)
                    .optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content").orEmpty()
                content.ifBlank { "The model returned an empty response. Try again." }
            } finally {
                conn.disconnect()
            }
        }

    private fun describeError(code: Int, body: String): String {
        val detail = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (_: Exception) { "" }
        val base = when (code) {
            401 -> "API key rejected (401). Check the key in Cal AI settings."
            402 -> "OpenRouter says this account is out of credits (402)."
            403 -> "Request forbidden (403)."
            404 -> "Model not found (404). The free router may have changed."
            429 -> "Rate limited (429). Free models allow only a few requests per minute."
            in 500..599 -> "OpenRouter had a server error ($code). Try again shortly."
            else -> "Request failed (HTTP $code)."
        }
        return if (detail.isBlank()) base else "$base\n$detail"
    }

    companion object {
        const val DEFAULT_MODEL = "openrouter/free"
        private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    }
}

/** A single chat turn in OpenAI-style role/content form. */
data class ChatMessage(val role: String, val content: String)
