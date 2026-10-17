package com.aistudio.vusin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.vusin.data.ChatMessage
import com.aistudio.vusin.data.GenAiRepository
import com.aistudio.vusin.data.MessageSender
import com.aistudio.vusin.data.ModelUiModel
import com.google.genai.kotlin.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.aistudio.vusin.ui.components.AvatarState
import kotlinx.coroutines.delay

enum class Screen {
    CHAT, GENERATE, MULTIMODAL, TOKENS, MODELS, SETTINGS
}

data class UiState(
    val currentScreen: Screen = Screen.CHAT,
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    
    // 3D Avatar state
    val avatarState: AvatarState = AvatarState.IDLE,
    val isAvatarVisible: Boolean = true,

    // Chat state
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.MODEL,
            text = "Hello! I am your 3D Gemini AI companion, powered by the Google Gen AI Kotlin SDK (VUSIN). Ask me anything!"
        )
    ),
    val isChatStreaming: Boolean = false,
    val selectedChatModel: String = GenAiRepository.DEFAULT_MODEL,
    
    // Generate state
    val generatePrompt: String = "Explain the architecture of Kotlin Multiplatform in 3 concise bullet points.",
    val generateResult: String = "",
    val isGenerating: Boolean = false,
    val temperature: Float = 0.7f,
    val systemInstruction: String = "You are a concise, helpful Android and Kotlin specialist.",
    val selectedGenerateModel: String = GenAiRepository.DEFAULT_MODEL,
    
    // Multimodal state
    val multimodalPrompt: String = "Describe what is happening in this image and provide 3 creative insights.",
    val multimodalResult: String = "",
    val isMultimodalLoading: Boolean = false,
    val selectedSampleImageIndex: Int = 0,
    
    // Tokens state
    val tokenPrompt: String = "Google Gen AI Kotlin SDK provides an idiomatic Kotlin interface for developers to integrate Gemini into Android and multiplatform applications.",
    val tokenCount: Long? = null,
    val isCountingTokens: Boolean = false,
    
    // Models state
    val modelsList: List<ModelUiModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GenAiRepository(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var activeChatSession: Chat? = null

    init {
        val key = repository.getApiKey()
        _uiState.update { it.copy(apiKey = key, hasApiKey = key.isNotBlank()) }
        loadModels()
        recountTokens(_uiState.value.tokenPrompt)
    }

    fun selectScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun updateApiKey(key: String) {
        repository.saveApiKey(key)
        _uiState.update { it.copy(apiKey = key, hasApiKey = key.isNotBlank()) }
        activeChatSession = null
        loadModels()
    }

    // --- Chat logic ---
    fun toggleAvatarVisibility() {
        _uiState.update { it.copy(isAvatarVisible = !it.isAvatarVisible) }
    }

    fun setAvatarState(state: AvatarState) {
        _uiState.update { it.copy(avatarState = state) }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isChatStreaming) return

        val lowerPrompt = userText.lowercase()
        val isGreetingOrPraise = lowerPrompt.contains("hi") || lowerPrompt.contains("hello") ||
                lowerPrompt.contains("hey") || lowerPrompt.contains("thanks") ||
                lowerPrompt.contains("thank") || lowerPrompt.contains("cool") ||
                lowerPrompt.contains("great") || lowerPrompt.contains("awesome")

        val userMessage = ChatMessage(sender = MessageSender.USER, text = userText)
        val initialAssistantMsg = ChatMessage(
            sender = MessageSender.MODEL,
            text = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage + initialAssistantMsg,
                isChatStreaming = true,
                avatarState = AvatarState.THINKING
            )
        }

        viewModelScope.launch {
            if (!_uiState.value.hasApiKey) {
                // Friendly mock / guidance response if no key configured
                delay(600)
                _uiState.update { it.copy(avatarState = AvatarState.SPEAKING) }
                val demoResponse = "To enable live responses, please enter your Gemini API Key in the Settings tab.\n\n" +
                        "This app uses the official Google Gen AI Kotlin SDK:\n" +
                        "```kotlin\nval client = Client(apiKey = \"YOUR_KEY\")\nval chat = client.chats.create(model = \"${_uiState.value.selectedChatModel}\")\nval response = chat.sendMessage(\"$userText\")\n```"
                updateLastChatMessage(demoResponse, isStreaming = false)
                _uiState.update { it.copy(isChatStreaming = false, avatarState = if (isGreetingOrPraise) AvatarState.HAPPY else AvatarState.IDLE) }
                if (isGreetingOrPraise) {
                    delay(2500)
                    _uiState.update { it.copy(avatarState = AvatarState.IDLE) }
                }
                return@launch
            }

            try {
                if (activeChatSession == null) {
                    activeChatSession = repository.createChat(model = _uiState.value.selectedChatModel)
                }

                val chat = activeChatSession
                if (chat == null) {
                    updateLastChatMessage("Failed to initialize chat session. Check your API key.", isStreaming = false)
                    _uiState.update { it.copy(isChatStreaming = false, avatarState = AvatarState.IDLE) }
                    return@launch
                }

                val accumulated = StringBuilder()
                var firstChunk = true
                val flow = chat.sendMessageStream(userText)
                flow.collect { response ->
                    response.text?.let { chunk ->
                        if (firstChunk) {
                            firstChunk = false
                            _uiState.update { it.copy(avatarState = AvatarState.SPEAKING) }
                        }
                        accumulated.append(chunk)
                        updateLastChatMessage(accumulated.toString(), isStreaming = true)
                    }
                }
                updateLastChatMessage(accumulated.toString(), isStreaming = false)
                if (isGreetingOrPraise) {
                    _uiState.update { it.copy(avatarState = AvatarState.HAPPY) }
                    delay(2500)
                }
            } catch (e: Exception) {
                updateLastChatMessage("Error: ${e.localizedMessage ?: e.message}", isStreaming = false)
            } finally {
                _uiState.update { it.copy(isChatStreaming = false, avatarState = AvatarState.IDLE) }
            }
        }
    }

    private fun updateLastChatMessage(text: String, isStreaming: Boolean) {
        _uiState.update { state ->
            val msgs = state.chatMessages.toMutableList()
            if (msgs.isNotEmpty()) {
                val last = msgs.removeAt(msgs.lastIndex)
                msgs.add(last.copy(text = text, isStreaming = isStreaming))
            }
            state.copy(chatMessages = msgs)
        }
    }

    fun clearChat() {
        activeChatSession = null
        _uiState.update {
            it.copy(
                chatMessages = listOf(
                    ChatMessage(
                        sender = MessageSender.MODEL,
                        text = "Chat history cleared. What would you like to explore next?"
                    )
                )
            )
        }
    }

    fun selectChatModel(model: String) {
        activeChatSession = null
        _uiState.update { it.copy(selectedChatModel = model) }
    }

    // --- Generate logic ---
    fun updateGeneratePrompt(prompt: String) {
        _uiState.update { it.copy(generatePrompt = prompt) }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp) }
    }

    fun updateSystemInstruction(instruction: String) {
        _uiState.update { it.copy(systemInstruction = instruction) }
    }

    fun selectGenerateModel(model: String) {
        _uiState.update { it.copy(selectedGenerateModel = model) }
    }

    fun startGenerateStream() {
        val prompt = _uiState.value.generatePrompt
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        _uiState.update { it.copy(isGenerating = true, generateResult = "", avatarState = AvatarState.THINKING) }

        viewModelScope.launch {
            if (!_uiState.value.hasApiKey) {
                delay(600)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        avatarState = AvatarState.IDLE,
                        generateResult = "Please configure your GEMINI_API_KEY in the Settings tab.\n\nCode snippet:\nval client = Client(apiKey = key)\nval resp = client.models.generateContent(\"${it.selectedGenerateModel}\", \"$prompt\")"
                    )
                }
                return@launch
            }

            val accumulated = StringBuilder()
            var first = true
            repository.generateContentStream(
                prompt = prompt,
                model = _uiState.value.selectedGenerateModel,
                temperature = _uiState.value.temperature,
                systemInstruction = _uiState.value.systemInstruction
            ).collect { chunk ->
                if (first) {
                    first = false
                    _uiState.update { it.copy(avatarState = AvatarState.SPEAKING) }
                }
                accumulated.append(chunk)
                _uiState.update { it.copy(generateResult = accumulated.toString()) }
            }
            _uiState.update { it.copy(isGenerating = false, avatarState = AvatarState.IDLE) }
        }
    }

    // --- Multimodal logic ---
    fun updateMultimodalPrompt(prompt: String) {
        _uiState.update { it.copy(multimodalPrompt = prompt) }
    }

    fun selectSampleImage(index: Int) {
        _uiState.update { it.copy(selectedSampleImageIndex = index) }
    }

    fun analyzeImageWithPrompt(imageBytes: ByteArray) {
        val prompt = _uiState.value.multimodalPrompt
        if (prompt.isBlank() || _uiState.value.isMultimodalLoading) return

        _uiState.update { it.copy(isMultimodalLoading = true, multimodalResult = "", avatarState = AvatarState.THINKING) }

        viewModelScope.launch {
            if (!_uiState.value.hasApiKey) {
                delay(600)
                _uiState.update {
                    it.copy(
                        isMultimodalLoading = false,
                        avatarState = AvatarState.IDLE,
                        multimodalResult = "Multimodal analysis requires a valid API key. Please set your Gemini API key in Settings.\n\nCode snippet:\nval content = Content(parts = listOf(Part(text = prompt), Part(inlineData = Blob(data = imageBytes, mimeType = \"image/jpeg\"))))\nclient.models.generateContent(\"gemini-3.8-flash\", contents = listOf(content))"
                    )
                }
                return@launch
            }

            val result = repository.generateMultimodal(
                prompt = prompt,
                imageBytes = imageBytes,
                mimeType = "image/jpeg",
                model = GenAiRepository.DEFAULT_MODEL
            )

            result.fold(
                onSuccess = { res ->
                    _uiState.update { it.copy(multimodalResult = res, isMultimodalLoading = false, avatarState = AvatarState.SPEAKING) }
                    delay(2000)
                    _uiState.update { it.copy(avatarState = AvatarState.IDLE) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(multimodalResult = "Error: ${err.message}", isMultimodalLoading = false, avatarState = AvatarState.IDLE) }
                }
            )
        }
    }

    // --- Tokens logic ---
    fun updateTokenPrompt(prompt: String) {
        _uiState.update { it.copy(tokenPrompt = prompt) }
        recountTokens(prompt)
    }

    private fun recountTokens(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.update { it.copy(tokenCount = 0L) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCountingTokens = true) }
            val res = repository.countTokens(prompt)
            res.fold(
                onSuccess = { count -> _uiState.update { it.copy(tokenCount = count, isCountingTokens = false) } },
                onFailure = { _uiState.update { it.copy(tokenCount = (prompt.length / 4L).coerceAtLeast(1L), isCountingTokens = false) } }
            )
        }
    }

    // --- Models logic ---
    fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true, modelsError = null) }
            val res = repository.listModels()
            res.fold(
                onSuccess = { list -> _uiState.update { it.copy(modelsList = list, isLoadingModels = false) } },
                onFailure = { err -> _uiState.update { it.copy(modelsError = err.message, isLoadingModels = false) } }
            )
        }
    }
}
