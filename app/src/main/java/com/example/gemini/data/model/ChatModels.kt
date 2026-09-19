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
    PRO("Gemini 3.1 Pro", "gemini-3.1-pro-preview", "Advanced reasoning, coding and analysis", "Thinking"),
    LIVE("Gemini 3.8 Live", "gemini-3.8-live", "Real-time Live API for voice conversations", "Live")
}

enum class ChatbotRole(
    val id: String,
    val displayName: String,
    val systemInstruction: String,
    val defaultModel: GeminiModel,
    val description: String,
    val iconName: String
) {
    GENERAL(
        id = "general",
        displayName = "General Assistant",
        systemInstruction = "You are Gemini, a helpful, intelligent, and versatile AI assistant. Answer the user's questions clearly, concisely, and accurately.",
        defaultModel = GeminiModel.FLASH_EXTENDED,
        description = "Versatile & friendly for any everyday question",
        iconName = "AutoAwesome"
    ),
    CODER(
        id = "coder",
        displayName = "Coding Expert",
        systemInstruction = "You are an elite software architecture engineer and programming expert. Provide precise, clean, production-grade code snippets with clear explanations and best practices. Adhere to optimal software design principles.",
        defaultModel = GeminiModel.PRO,
        description = "Write, refactor, and debug complex code",
        iconName = "Code"
    ),
    REASONING(
        id = "reasoning",
        displayName = "Math & Logic Coach",
        systemInstruction = "You are a senior mathematician and logical reasoning instructor. Break down complex, analytical, scientific, or mathematical problems step-by-step. Focus on logical derivation and clarity.",
        defaultModel = GeminiModel.PRO,
        description = "Step-by-step logic and mathematical analysis",
        iconName = "Functions"
    ),
    FAST(
        id = "fast",
        displayName = "Fast Summarizer",
        systemInstruction = "You are a high-speed text processing agent. Provide extremely rapid, concise summaries, bullet points, grammar corrections, or translations. Keep descriptions highly direct and minimal.",
        defaultModel = GeminiModel.FLASH_LITE,
        description = "Ultra-quick answers and text operations",
        iconName = "Zap"
    )
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
    val isPinned: Boolean = false,
    val chatbotRoleId: String = "general"
)
