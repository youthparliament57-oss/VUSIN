package com.aistudio.vusin.data

import android.content.Context
import com.aistudio.vusin.BuildConfig
import com.google.genai.kotlin.Chat
import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Blob
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.ListModelsConfig
import com.google.genai.kotlin.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class MessageSender {
    USER, MODEL, SYSTEM
}

data class ModelUiModel(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val inputTokenLimit: Long?,
    val outputTokenLimit: Long?,
    val category: String = "General",
    val badge: String = "LATEST"
)

class GenAiRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("vusin_genai_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_API_KEY = "api_key"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val POPULAR_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.1-pro",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-4-argon"
        )

        val DEFAULT_MODELS_METADATA = listOf(
            ModelUiModel(
                id = "gemini-3.8-flash",
                name = "models/gemini-3.8-flash",
                displayName = "Gemini 3.8 Flash",
                description = "Flagship 2026 model. Engineered for complex workflows, fast software engineering & low-latency streaming.",
                inputTokenLimit = 2097152L,
                outputTokenLimit = 16384L,
                category = "General / Coding",
                badge = "FLAGSHIP 2026"
            ),
            ModelUiModel(
                id = "gemini-3.1-pro",
                name = "models/gemini-3.1-pro",
                displayName = "Gemini 3.1 Pro",
                description = "Advanced reasoning, frontier math, complex multi-turn logic and deep code synthesis.",
                inputTokenLimit = 2097152L,
                outputTokenLimit = 16384L,
                category = "Deep Reasoning",
                badge = "PRO REASONING"
            ),
            ModelUiModel(
                id = "gemini-3.5-flash",
                name = "models/gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash",
                description = "High-throughput multimodal foundation model with superior speed and multimodal perception.",
                inputTokenLimit = 1048576L,
                outputTokenLimit = 8192L,
                category = "Multimodal",
                badge = "BALANCED"
            ),
            ModelUiModel(
                id = "gemini-3.5-flash-lite",
                name = "models/gemini-3.5-flash-lite",
                displayName = "Gemini 3.5 Flash-Lite",
                description = "Ultra-low latency and highly cost-efficient model designed for instant mobile query answering.",
                inputTokenLimit = 1048576L,
                outputTokenLimit = 8192L,
                category = "Mobile / Edge",
                badge = "ULTRA FAST"
            ),
            ModelUiModel(
                id = "gemini-2.5-pro",
                name = "models/gemini-2.5-pro",
                displayName = "Gemini 2.5 Pro",
                description = "Established reasoning champion for deep analytical tasks and long-context processing.",
                inputTokenLimit = 1048576L,
                outputTokenLimit = 8192L,
                category = "Reasoning",
                badge = "STABLE PRO"
            ),
            ModelUiModel(
                id = "gemini-2.5-flash",
                name = "models/gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                description = "High-efficiency 2.5 generation model for general-purpose high-volume workloads.",
                inputTokenLimit = 1048576L,
                outputTokenLimit = 8192L,
                category = "General",
                badge = "STABLE"
            ),
            ModelUiModel(
                id = "gemini-4-argon",
                name = "models/gemini-4-argon",
                displayName = "Gemini 4 Argon",
                description = "Next-generation frontier preview model pushing boundaries of autonomous multi-step reasoning.",
                inputTokenLimit = 4194304L,
                outputTokenLimit = 32768L,
                category = "Next-Gen",
                badge = "NEXT-GEN PREVIEW"
            )
        )
    }

    fun getApiKey(): String {
        val saved = prefs.getString(PREF_API_KEY, "") ?: ""
        return if (saved.isNotBlank()) saved else BuildConfig.GEMINI_API_KEY
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(PREF_API_KEY, key.trim()).apply()
    }

    private fun getClient(): Client {
        val key = getApiKey()
        return if (key.isNotBlank()) {
            Client(apiKey = key)
        } else {
            Client()
        }
    }

    suspend fun generateContent(
        prompt: String,
        model: String = DEFAULT_MODEL,
        temperature: Float? = null,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            if (key.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings.")
                )
            }
            val client = getClient()
            val config = GenerateContentConfig(
                temperature = temperature?.toDouble(),
                systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { Content.fromText(it) }
            )
            val response = client.models.generateContent(
                model = model,
                text = prompt,
                config = config
            )
            val text = response.text ?: "(No response text returned)"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateContentStream(
        prompt: String,
        model: String = DEFAULT_MODEL,
        temperature: Float? = null,
        systemInstruction: String? = null
    ): Flow<String> = flow {
        val key = getApiKey()
        if (key.isBlank()) {
            emit("Error: Gemini API key is not configured. Please configure it in the Settings screen.")
            return@flow
        }
        try {
            val client = getClient()
            val config = GenerateContentConfig(
                temperature = temperature?.toDouble(),
                systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { Content.fromText(it) }
            )
            val responseFlow = client.models.generateContentStream(
                model = model,
                text = prompt,
                config = config
            )
            responseFlow.collect { resp ->
                val chunk = resp.text
                if (!chunk.isNullOrEmpty()) {
                    emit(chunk)
                }
            }
        } catch (e: Exception) {
            emit("\n[Error occurred: ${e.localizedMessage ?: e.message}]")
        }
    }.flowOn(Dispatchers.IO)

    fun createChat(
        model: String = DEFAULT_MODEL,
        systemInstruction: String? = null
    ): Chat? {
        val key = getApiKey()
        if (key.isBlank()) return null
        return try {
            val client = getClient()
            val config = GenerateContentConfig(
                systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { Content.fromText(it) }
            )
            client.chats.create(model = model, config = config)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun countTokens(
        prompt: String,
        model: String = DEFAULT_MODEL
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            if (key.isBlank()) {
                // Approximate fallback: ~4 chars per token
                return@withContext Result.success((prompt.length / 4L).coerceAtLeast(1L))
            }
            val client = getClient()
            val response = client.models.countTokens(model = model, text = prompt)
            val tokens = response.totalTokens ?: (prompt.length / 4L)
            Result.success(tokens.toLong())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listModels(): Result<List<ModelUiModel>> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            if (key.isBlank()) {
                // Return verified 2026 model lineup if not authenticated
                return@withContext Result.success(DEFAULT_MODELS_METADATA)
            }
            val client = getClient()
            val modelsList = mutableListOf<ModelUiModel>()
            val pager = client.models.list(ListModelsConfig(pageSize = 30))
            pager.collect { m ->
                val id = m.name?.removePrefix("models/") ?: "unknown"
                val badge = when {
                    id.contains("3.8") -> "FLAGSHIP 2026"
                    id.contains("3.1") -> "PRO REASONING"
                    id.contains("3.5") && id.contains("lite") -> "ULTRA FAST"
                    id.contains("3.5") -> "BALANCED"
                    id.contains("4") -> "NEXT-GEN"
                    id.contains("pro") -> "PRO"
                    else -> "STABLE"
                }
                modelsList.add(
                    ModelUiModel(
                        id = id,
                        name = m.name ?: id,
                        displayName = m.displayName ?: id.replace("-", " ").capitalizeWords(),
                        description = m.description ?: "Google Gemini frontier foundation model.",
                        inputTokenLimit = m.inputTokenLimit?.toLong() ?: 1048576L,
                        outputTokenLimit = m.outputTokenLimit?.toLong() ?: 8192L,
                        category = if (id.contains("pro")) "Reasoning" else "General",
                        badge = badge
                    )
                )
            }
            if (modelsList.isEmpty()) {
                Result.success(DEFAULT_MODELS_METADATA)
            } else {
                Result.success(modelsList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateMultimodal(
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        model: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            if (key.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings.")
                )
            }
            val client = getClient()
            val content = Content(
                parts = listOf(
                    Part(text = prompt),
                    Part(inlineData = Blob(data = imageBytes, mimeType = mimeType))
                )
            )
            val response = client.models.generateContent(
                model = model,
                contents = listOf(content)
            )
            val text = response.text ?: "(No multimodal response returned)"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
