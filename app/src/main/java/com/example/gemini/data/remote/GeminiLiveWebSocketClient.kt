package com.example.gemini.data.remote

import android.util.Base64
import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class GeminiLiveWebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Allow long-running streaming connections
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun connect(
        apiKey: String,
        modelName: String, // e.g. "models/gemini-2.0-flash-exp"
        voiceName: String, // e.g. "Aoede", "Charon", "Fenrir", "Kore", "Puck"
        systemInstruction: String?,
        onAudioDataReceived: (ByteArray) -> Unit,
        onTextReceived: (String) -> Unit,
        onSessionConfigured: () -> Unit,
        onTurnComplete: (Boolean) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Construct the correct secure WebSockets URL for the Gemini Live API
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    // Send BidiGenerateContentSetup message to configure the voice agent
                    val setupMessage = JSONObject().apply {
                        put("setup", JSONObject().apply {
                            put("model", modelName)
                            put("generationConfig", JSONObject().apply {
                                put("responseModalities", JSONArray().apply {
                                    put("AUDIO")
                                })
                                put("speechConfig", JSONObject().apply {
                                    put("voiceConfig", JSONObject().apply {
                                        put("prebuiltVoiceConfig", JSONObject().apply {
                                            put("voiceName", voiceName)
                                        })
                                    })
                                })
                            })
                            if (!systemInstruction.isNullOrBlank()) {
                                put("systemInstruction", JSONObject().apply {
                                    put("parts", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("text", systemInstruction)
                                        })
                                    })
                                })
                            }
                        })
                    }
                    webSocket.send(setupMessage.toString())
                    onSessionConfigured()
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    
                    // Parse serverContent for audio chunks and textual transcription
                    val serverContent = json.optJSONObject("serverContent")
                    if (serverContent != null) {
                        val turnComplete = serverContent.optBoolean("turnComplete", false)
                        if (turnComplete) {
                            onTurnComplete(true)
                        }

                        val modelTurn = serverContent.optJSONObject("modelTurn")
                        if (modelTurn != null) {
                            onTurnComplete(false) // Server started speaking/sending new content
                            val parts = modelTurn.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    
                                    // Extract the real-time model audio payload
                                    val inlineData = part.optJSONObject("inlineData")
                                    if (inlineData != null) {
                                        val dataBase64 = inlineData.optString("data")
                                        if (dataBase64.isNotEmpty()) {
                                            val pcmBytes = Base64.decode(dataBase64, Base64.DEFAULT)
                                            onAudioDataReceived(pcmBytes)
                                        }
                                    }
                                    
                                    // Extract real-time transcript
                                    val partText = part.optString("text")
                                    if (partText.isNotEmpty()) {
                                        onTextReceived(partText)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiLiveWS", "Error processing incoming Live message: ${e.message}", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handled if server uses binary payloads
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Closed
            }
        })
    }

    fun sendAudioChunk(pcmData: ByteArray) {
        val base64Data = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        try {
            val audioMessage = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm")
                            put("data", base64Data)
                        })
                    })
                })
            }
            webSocket?.send(audioMessage.toString())
        } catch (e: Exception) {
            Log.e("GeminiLiveWS", "Error transmitting audio frame: ${e.message}", e)
        }
    }

    fun close() {
        webSocket?.close(1000, "Dismissed")
        webSocket = null
    }
}
