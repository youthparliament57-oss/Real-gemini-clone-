package com.example.gemini.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.gemini.data.model.ChatMessage
import com.example.gemini.data.model.GeminiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        messages: List<ChatMessage>,
        newPrompt: String,
        bitmap: Bitmap? = null,
        model: GeminiModel = GeminiModel.FLASH_EXTENDED,
        customApiKey: String? = null,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }

        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                Exception("API Key Missing: Please enter your Google Gemini API Key in the Settings menu (pencil icon on top-right) to start chatting with real-time AI models.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Add previous conversational context (last 6 messages max for context window efficiency)
            val recentMessages = messages.takeLast(6)
            for (msg in recentMessages) {
                val role = if (msg.isUser) "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.content)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current user turn
            val userContent = JSONObject()
            userContent.put("role", "user")
            val userParts = JSONArray()

            val textPart = JSONObject()
            textPart.put("text", newPrompt)
            userParts.put(textPart)

            if (bitmap != null) {
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", bitmapToBase64(bitmap))
                imagePart.put("inlineData", inlineData)
                userParts.put(imagePart)
            }

            userContent.put("parts", userParts)
            contentsArray.put(userContent)

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)

            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)
            generationConfig.put("topP", 0.95)
            rootJson.put("generationConfig", generationConfig)

            if (!systemInstruction.isNullOrBlank()) {
                val systemInstructionObj = JSONObject()
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", systemInstruction)
                partsArray.put(partObj)
                systemInstructionObj.put("parts", partsArray)
                rootJson.put("systemInstruction", systemInstructionObj)
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val firstPart = parts.getJSONObject(0)
                        val text = firstPart.optString("text")
                        if (text.isNotEmpty()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("Empty response received from Gemini API."))
            } else {
                val parsedErrorMsg = try {
                    val errorJson = JSONObject(responseBody ?: "")
                    val errorObj = errorJson.optJSONObject("error")
                    errorObj?.optString("message") ?: "HTTP Error ${response.code}"
                } catch (e: Exception) {
                    responseBody ?: response.message
                }
                
                val userFriendlyMessage = when (response.code) {
                    403 -> "Invalid API Key: The Gemini API key you provided is incorrect, restricted, or unauthorized. ($parsedErrorMsg)"
                    429 -> "Quota Exceeded: You have exceeded the free limit. Please wait a minute or use a different key. ($parsedErrorMsg)"
                    404 -> "Model Not Found: The selected model is unavailable in your region. ($parsedErrorMsg)"
                    else -> "API request failed with error code ${response.code}: $parsedErrorMsg"
                }
                Result.failure(Exception(userFriendlyMessage))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Call failed", e)
            Result.failure(Exception("Network/Connection failure: Please verify your internet connection and try again. Detailed: ${e.localizedMessage}"))
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
