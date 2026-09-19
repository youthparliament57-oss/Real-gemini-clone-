package com.example.gemini.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemini.data.model.ChatMessage
import com.example.gemini.data.model.ChatSession
import com.example.gemini.data.model.GeminiModel
import com.example.gemini.data.model.ChatbotRole
import com.example.gemini.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    val sessions: StateFlow<List<ChatSession>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _currentModel = MutableStateFlow(GeminiModel.FLASH_EXTENDED)
    val currentModel: StateFlow<GeminiModel> = _currentModel.asStateFlow()

    private val _currentSessionRole = MutableStateFlow(ChatbotRole.GENERAL)
    val currentSessionRole: StateFlow<ChatbotRole> = _currentSessionRole.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isLiveOpen = MutableStateFlow(false)
    val isLiveOpen: StateFlow<Boolean> = _isLiveOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var messagesJob: Job? = null

    init {
        // Load persisted API key from SharedPreferences
        val prefs = application.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)
        _customApiKey.value = prefs.getString("custom_api_key", "") ?: ""

        // Initialize TextToSpeech engine
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.getDefault()
                textToSpeech?.language = locale
                
                // Select highest quality neural/wavenet voice if available
                try {
                    val availableVoices = textToSpeech?.voices
                    if (!availableVoices.isNullOrEmpty()) {
                        val bestVoice = availableVoices.filter { voice ->
                            voice.locale.language == locale.language
                        }.maxByOrNull { voice ->
                            var score = 0
                            val name = voice.name.lowercase()
                            if (name.contains("neural") || name.contains("wavenet") || name.contains("network")) score += 10
                            if (voice.features.contains("neural") || voice.features.contains("wavenet")) score += 5
                            if (voice.quality >= 400) score += 3
                            score
                        }
                        bestVoice?.let {
                            textToSpeech?.voice = it
                        }
                    }
                } catch (e: Exception) {
                    // Fallback gracefully
                }
            }
        }

        // Combine sessions list and current sessionId to track the current active chatbot role dynamically
        viewModelScope.launch {
            combine(repository.sessions, _currentSessionId) { list, id ->
                list.find { it.id == id }?.chatbotRoleId
            }.collect { roleId ->
                val newRole = ChatbotRole.values().find { it.id == roleId } ?: ChatbotRole.GENERAL
                _currentSessionRole.value = newRole
            }
        }

        // Initialize with either latest session or create a clean one
        viewModelScope.launch {
            val existing = repository.sessions.firstOrNull()?.firstOrNull()
            if (existing != null) {
                selectSession(existing.id)
            } else {
                startNewChat()
            }
        }
    }

    fun selectSession(sessionId: String) {
        if (_currentSessionId.value == sessionId && messagesJob?.isActive == true) return
        _currentSessionId.value = sessionId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(sessionId).collect { msgs ->
                // Maintain the thinking placeholder if active
                if (_isThinking.value) {
                    val containsThinking = msgs.any { it.isThinking }
                    if (!containsThinking) {
                        _currentMessages.value = msgs + ChatMessage(
                            sessionId = sessionId,
                            isUser = false,
                            content = "",
                            isThinking = true
                        )
                        return@collect
                    }
                }
                _currentMessages.value = msgs
            }
        }
        // Sync model when switching sessions
        viewModelScope.launch {
            val sList = repository.sessions.firstOrNull()
            val currentSessionObj = sList?.find { it.id == sessionId }
            currentSessionObj?.let {
                val role = ChatbotRole.values().find { r -> r.id == it.chatbotRoleId } ?: ChatbotRole.GENERAL
                _currentModel.value = role.defaultModel
            }
        }
    }

    fun startNewChat(chatbotRoleId: String = "general") {
        viewModelScope.launch {
            val role = ChatbotRole.values().find { it.id == chatbotRoleId } ?: ChatbotRole.GENERAL
            val newSession = repository.createNewSession("New chat", chatbotRoleId)
            _currentSessionId.value = newSession.id
            _currentMessages.value = emptyList()
            _inputText.value = ""
            _selectedImageUri.value = null
            selectModel(role.defaultModel)
            
            // Instantly trigger active message tracking for this brand-new session
            selectSession(newSession.id)
        }
    }

    fun selectSessionRole(role: ChatbotRole) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.updateSessionRole(sessionId, role.id)
            selectModel(role.defaultModel)
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun selectModel(model: GeminiModel) {
        _currentModel.value = model
    }

    fun openLiveMode() {
        _isLiveOpen.value = true
    }

    fun closeLiveMode() {
        _isLiveOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        val prefs = getApplication<Application>().getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = customPrompt ?: _inputText.value.trim()
        if (prompt.isBlank() && _selectedImageUri.value == null) return

        val sessionId = _currentSessionId.value ?: return
        val imageUri = _selectedImageUri.value

        // Clear input bar
        _inputText.value = ""
        _selectedImageUri.value = null

        viewModelScope.launch {
            try {
                // 1. Save user message to database
                val userMsg = repository.saveUserMessage(
                    sessionId = sessionId,
                    content = prompt,
                    imageUri = imageUri?.toString()
                )

                // Add placeholder thinking message in UI
                _isThinking.value = true
                val thinkingMsg = ChatMessage(
                    sessionId = sessionId,
                    isUser = false,
                    content = "",
                    isThinking = true
                )
                _currentMessages.value = _currentMessages.value + thinkingMsg

                // Load Bitmap if image was attached
                val bitmap = imageUri?.let { uri ->
                    loadBitmapFromUri(getApplication(), uri)
                }

                // Call Gemini API passing the system instruction for the chosen role
                val systemInstruction = _currentSessionRole.value.systemInstruction
                val result = repository.callGemini(
                    history = _currentMessages.value.filter { !it.isThinking },
                    prompt = prompt,
                    bitmap = bitmap,
                    model = _currentModel.value,
                    customApiKey = _customApiKey.value,
                    systemInstruction = systemInstruction
                )

                // Save AI message to database
                val responseText = result.getOrElse {
                    "Unable to get response from Gemini. Please check your network connection or API key."
                }
                repository.saveAiResponse(sessionId, responseText)
            } catch (e: Exception) {
                repository.saveAiResponse(sessionId, "Error occurred: ${e.localizedMessage ?: "Unknown compilation or execution issue."}")
            } finally {
                _isThinking.value = false
                _currentMessages.value = _currentMessages.value.filter { !it.isThinking }
            }
        }
    }

    fun modifyResponse(message: ChatMessage, option: String) {
        val prompt = when (option.lowercase()) {
            "shorter" -> "Please make this response more concise and to the point: \"${message.content}\""
            "longer" -> "Please explain this response with more detail and examples: \"${message.content}\""
            "simpler" -> "Please explain this in simpler, easier-to-understand terms: \"${message.content}\""
            "more casual" -> "Please rephrase this in a friendly and casual tone: \"${message.content}\""
            "more professional" -> "Please rephrase this in a formal, professional tone: \"${message.content}\""
            else -> "Please improve and refine this response: \"${message.content}\""
        }
        sendMessage(prompt)
    }

    fun toggleLike(messageId: String, isLiked: Boolean?) {
        viewModelScope.launch {
            repository.updateMessageLike(messageId, isLiked)
            _currentMessages.value = _currentMessages.value.map {
                if (it.id == messageId) it.copy(isLiked = isLiked) else it
            }
        }
    }

    fun togglePinSession(session: ChatSession) {
        viewModelScope.launch {
            repository.togglePinSession(session)
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = repository.sessions.firstOrNull()?.filter { it.id != sessionId }
                if (!remaining.isNullOrEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    startNewChat()
                }
            }
        }
    }

    fun speakMessage(messageId: String, text: String) {
        if (_currentlySpeakingMessageId.value == messageId) {
            textToSpeech?.stop()
            _currentlySpeakingMessageId.value = null
        } else {
            textToSpeech?.stop()
            _currentlySpeakingMessageId.value = messageId
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
