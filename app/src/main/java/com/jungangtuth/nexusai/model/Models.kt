package com.jungangtuth.nexusai.model

data class ProviderConfig(
    val id: String,
    val name: String,
    val enabled: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "",
    val defaultModel: String = ""
)

data class ChatMessage(val role: String, val content: String, val model: String? = null)
data class Conversation(val id: Long = 0, val title: String, val updatedAt: Long = System.currentTimeMillis())
