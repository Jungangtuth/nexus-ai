package com.jungangtuth.nexusai.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jungangtuth.nexusai.model.ProviderConfig

class SecureStore(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "nexus_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val gson = Gson()

    fun getProviders(): List<ProviderConfig> {
        val json = prefs.getString("providers", null) ?: return defaultProviders()
        return runCatching {
            val type = object : TypeToken<List<ProviderConfig>>() {}.type
            gson.fromJson<List<ProviderConfig>>(json, type)
        }.getOrElse { defaultProviders() }
    }

    fun saveProviders(list: List<ProviderConfig>) {
        prefs.edit().putString("providers", gson.toJson(list)).apply()
    }

    fun defaultProviders() = listOf(
        ProviderConfig("openai", "OpenAI", false, "", "https://api.openai.com/v1", "gpt-4o-mini"),
        ProviderConfig("gemini", "Google Gemini", false, "", "https://generativelanguage.googleapis.com/v1beta", "gemini-1.5-flash"),
        ProviderConfig("anthropic", "Anthropic Claude", false, "", "https://api.anthropic.com/v1", "claude-3-5-sonnet-latest"),
        ProviderConfig("deepseek", "DeepSeek", false, "", "https://api.deepseek.com", "deepseek-chat"),
        ProviderConfig("openrouter", "OpenRouter", false, "", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini")
    )
}
