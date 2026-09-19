package com.example.gemini.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.gemini.data.local.ChatDao
import com.example.gemini.data.local.ChatDatabase
import com.example.gemini.data.local.ChatMessageEntity
import com.example.gemini.data.local.ChatSessionEntity
import com.example.gemini.data.model.ChatMessage
import com.example.gemini.data.model.ChatSession
import com.example.gemini.data.model.GeminiModel
import com.example.gemini.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(context: Context) {
    private val dao: ChatDao = ChatDatabase.getDatabase(context).chatDao()
    private val geminiService = GeminiService()

    val sessions: Flow<List<ChatSession>> = dao.getAllSessions().map { list ->
        list.map { entity ->
            ChatSession(
                id = entity.id,
                title = entity.title,
                updatedAt = entity.updatedAt,
                isPinned = entity.isPinned,
                chatbotRoleId = entity.chatbotRoleId
            )
        }
    }

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> =
        dao.getMessagesForSession(sessionId).map { list ->
            list.map { entity ->
                ChatMessage(
                    id = entity.id,
                    sessionId = entity.sessionId,
                    isUser = entity.isUser,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    imageUri = entity.imageUri,
                    isLiked = entity.isLiked
                )
            }
        }

    suspend fun createNewSession(initialTitle: String = "New chat", chatbotRoleId: String = "general"): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = initialTitle,
            updatedAt = System.currentTimeMillis(),
            chatbotRoleId = chatbotRoleId
        )
        dao.insertSession(
            ChatSessionEntity(
                id = session.id,
                title = session.title,
                updatedAt = session.updatedAt,
                isPinned = false,
                chatbotRoleId = session.chatbotRoleId
            )
        )
        return session
    }

    suspend fun saveUserMessage(
        sessionId: String,
        content: String,
        imageUri: String? = null
    ): ChatMessage {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            isUser = true,
            content = content,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )
        dao.insertMessage(
            ChatMessageEntity(
                id = userMsg.id,
                sessionId = userMsg.sessionId,
                isUser = true,
                content = userMsg.content,
                timestamp = userMsg.timestamp,
                imageUri = userMsg.imageUri
            )
        )
        // Update session timestamp and title if it's the first message
        val titleSnippet = if (content.length > 28) content.take(28) + "…" else content
        val existing = dao.getSessionById(sessionId)
        if (existing != null) {
            dao.updateSession(
                existing.copy(
                    title = titleSnippet,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return userMsg
    }

    suspend fun saveAiResponse(
        sessionId: String,
        content: String
    ): ChatMessage {
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            isUser = false,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(
            ChatMessageEntity(
                id = aiMsg.id,
                sessionId = aiMsg.sessionId,
                isUser = false,
                content = aiMsg.content,
                timestamp = aiMsg.timestamp
            )
        )
        return aiMsg
    }

    suspend fun callGemini(
        history: List<ChatMessage>,
        prompt: String,
        bitmap: Bitmap? = null,
        model: GeminiModel = GeminiModel.FLASH_EXTENDED,
        customApiKey: String? = null,
        systemInstruction: String? = null
    ): Result<String> {
        return geminiService.generateContent(
            messages = history,
            newPrompt = prompt,
            bitmap = bitmap,
            model = model,
            customApiKey = customApiKey,
            systemInstruction = systemInstruction
        )
    }

    suspend fun updateMessageLike(messageId: String, isLiked: Boolean?) {
        dao.updateMessageLike(messageId, isLiked)
    }

    suspend fun togglePinSession(session: ChatSession) {
        val existing = dao.getSessionById(session.id)
        if (existing != null) {
            dao.updateSession(
                existing.copy(
                    isPinned = !existing.isPinned
                )
            )
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        val existing = dao.getSessionById(sessionId)
        if (existing != null) {
            dao.updateSession(
                existing.copy(
                    title = newTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateSessionRole(sessionId: String, roleId: String) {
        dao.updateSessionRole(sessionId, roleId)
    }

    suspend fun deleteSession(sessionId: String) {
        dao.deleteMessagesForSession(sessionId)
        dao.deleteSession(sessionId)
    }
}
