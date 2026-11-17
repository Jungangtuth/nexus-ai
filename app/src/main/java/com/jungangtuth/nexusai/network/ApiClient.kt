package com.jungangtuth.nexusai.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.jungangtuth.nexusai.model.ChatMessage
import com.jungangtuth.nexusai.model.ProviderConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient {
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()

    fun sendMessage(provider: ProviderConfig, messages: List<ChatMessage>): String {
        if (provider.apiKey.isBlank()) return "API key belum diatur untuk ${provider.name}. Buka Pengaturan untuk menambahkannya."
        return try {
            when (provider.id) {
                "gemini" -> sendGemini(provider, messages)
                "anthropic" -> sendAnthropic(provider, messages)
                else -> sendOpenAICompatible(provider, messages)
            }
        } catch (e: Exception) {
            "Error (${provider.name}): ${e.message}"
        }
    }

    private fun sendOpenAICompatible(p: ProviderConfig, messages: List<ChatMessage>): String {
        val body = mapOf(
            "model" to p.defaultModel,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) }
        )
        val req = Request.Builder()
            .url(p.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${p.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return "HTTP ${resp.code}: ${s.take(300)}"
            val json = JsonParser.parseString(s).asJsonObject
            return json.getAsJsonArray("choices")[0].asJsonObject
                .getAsJsonObject("message").get("content").asString
        }
    }

    private fun sendAnthropic(p: ProviderConfig, messages: List<ChatMessage>): String {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val msgs = messages.filter { it.role != "system" }.map { mapOf("role" to it.role, "content" to it.content) }
        val body = mutableMapOf<String, Any>("model" to p.defaultModel, "max_tokens" to 1024, "messages" to msgs)
        if (system.isNotBlank()) body["system"] = system
        val req = Request.Builder()
            .url(p.baseUrl.trimEnd('/') + "/messages")
            .addHeader("x-api-key", p.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return "HTTP ${resp.code}: ${s.take(300)}"
            val json = JsonParser.parseString(s).asJsonObject
            return json.getAsJsonArray("content")[0].asJsonObject.get("text").asString
        }
    }

    private fun sendGemini(p: ProviderConfig, messages: List<ChatMessage>): String {
        val contents = messages.filter { it.role != "system" }.map {
            mapOf("role" to if (it.role == "assistant") "model" else "user",
                  "parts" to listOf(mapOf("text" to it.content)))
        }
        val body = mapOf("contents" to contents)
        val url = "${p.baseUrl.trimEnd('/')}/models/${p.defaultModel}:generateContent?key=${p.apiKey}"
        val req = Request.Builder().url(url).post(gson.toJson(body).toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return "HTTP ${resp.code}: ${s.take(300)}"
            val json = JsonParser.parseString(s).asJsonObject
            return json.getAsJsonArray("candidates")[0].asJsonObject
                .getAsJsonObject("content").getAsJsonArray("parts")[0].asJsonObject.get("text").asString
        }
    }

    fun testConnection(p: ProviderConfig): String = sendMessage(p, listOf(ChatMessage("user","Reply with OK")))
}
