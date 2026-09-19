package com.example.gemini.data.model

import java.util.UUID

enum class GeminiModel(
    val displayName: String,
    val modelId: String,
    val description: String,
    val badge: String = "Fast"
) {
    FLASH_EXTENDED("Flash Extended", "gemini-3.5-flash", "Fast & versatile for most tasks", "Recommended"),
    FLASH_LITE("Flash Lite", "gemini-3.1-flash-lite-preview", "Lightweight, ultra-fast responses", "Fast"),
    PRO("Gemini 3.1 Pro", "gemini-3.1-pro-preview", "Advanced reasoning, coding and analysis", "Thinking")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val isUser: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val isLiked: Boolean? = null,
    val isThinking: Boolean = false
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
