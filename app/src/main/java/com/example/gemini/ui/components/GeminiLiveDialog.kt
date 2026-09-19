package com.example.gemini.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

data class LiveTranscriptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isLiked: Boolean? = null
)

@Composable
fun GeminiLiveDialog(
    currentModel: com.example.gemini.data.model.GeminiModel,
    onDismiss: () -> Unit,
    onSpeakToChat: (String) -> Unit,
    onMinimizeToBubble: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isMuted by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(true) }
    var liveSpokenText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showVoiceSelector by remember { mutableStateOf(false) }
    var showScreenShareSheet by remember { mutableStateOf(false) }
    var currentVoice by remember { mutableStateOf(AvailableGeminiVoices.first()) }
    var speechAmplitude by remember { mutableStateOf(0f) }
    var isThinking by remember { mutableStateOf(false) }
    val geminiService = remember { com.example.gemini.data.remote.GeminiService() }
    var transcriptHistory by remember { mutableStateOf(listOf<LiveTranscriptItem>()) }
    val listState = rememberLazyListState()

    // Speech & Audio Engine references
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    // True Bidirectional WebSockets Gemini Live references
    var isWebSocketConnected by remember { mutableStateOf(false) }
    val webSocketClient = remember { com.example.gemini.data.remote.GeminiLiveWebSocketClient() }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var recordingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val recordingScope = rememberCoroutineScope()

    fun speakWithGeminiVoice(text: String, voice: GeminiVoice) {
        ttsEngine?.setPitch(voice.pitch)
        ttsEngine?.setSpeechRate(voice.speechRate)
        
        // Match the selected voice characteristics with Google's high quality neural voices
        try {
            val systemVoices = ttsEngine?.voices
            if (!systemVoices.isNullOrEmpty()) {
                val defaultLocale = Locale.getDefault()
                val matchedSystemVoice = systemVoices.filter { v ->
                    v.locale.language == defaultLocale.language
                }.maxByOrNull { v ->
                    var score = 0
                    val name = v.name.lowercase()
                    if (name.contains("neural") || name.contains("wavenet") || name.contains("network")) score += 10
                    if (v.features.contains("neural") || v.features.contains("wavenet")) score += 5
                    
                    val isMale = voice.id in listOf("ursa", "dipper", "orion")
                    if (isMale && (name.contains("male") || name.contains("guy") || name.contains("man"))) score += 5
                    if (!isMale && (name.contains("female") || name.contains("girl") || name.contains("woman"))) score += 5
                    
                    if (name.contains(voice.id.take(2).lowercase())) score += 8
                    score
                }
                matchedSystemVoice?.let {
                    ttsEngine?.voice = it
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully
        }
        
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gemini_live_tts")
        isSpeaking = true
    }

    fun startListening() {
        if (isMuted || isSpeaking || isThinking) return
        try {
            // Clean up any existing recognizer first to prevent locking up the microphone!
            speechRecognizer?.let {
                it.stopListening()
                it.destroy()
            }
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        isThinking = false
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val target = ((rmsdB + 2.0f) / 13.0f).coerceIn(0.0f, 1.0f)
                        speechAmplitude = speechAmplitude * 0.65f + target * 0.35f
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            liveSpokenText = text
                            transcriptHistory = transcriptHistory + LiveTranscriptItem(isUser = true, text = text)
                            
                            // Transition cleanly to Thinking State
                            isThinking = true
                            isListening = false
                            
                            // Launch a background coroutine to request Gemini content using full thread-safety
                            coroutineScope.launch {
                                val mappedContext = transcriptHistory.map {
                                    com.example.gemini.data.model.ChatMessage(
                                        sessionId = "live",
                                        isUser = it.isUser,
                                        content = it.text
                                    )
                                }
                                val result = geminiService.generateContent(
                                    messages = mappedContext,
                                    newPrompt = text,
                                    model = currentModel
                                )
                                
                                isThinking = false
                                val response = result.getOrElse { "Sorry, I had trouble processing that. Can you repeat it?" }
                                transcriptHistory = transcriptHistory + LiveTranscriptItem(isUser = false, text = response)
                                speakWithGeminiVoice(response, currentVoice)
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { liveSpokenText = it }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer.startListening(intent)
                speechRecognizer = recognizer
            }
        } catch (e: Exception) {
            isThinking = false
            isListening = false
        }
    }

    fun startRealLiveSession() {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY")) {
            startListening()
            return
        }

        val targetVoiceName = when (currentVoice.id) {
            "nova" -> "Aoede"
            "ursa" -> "Charon"
            "vega" -> "Kore"
            "lyra" -> "Aoede"
            "dipper" -> "Puck"
            "eclipse" -> "Charon"
            "orion" -> "Fenrir"
            "pegasus" -> "Kore"
            "orbit" -> "Puck"
            else -> "Aoede"
        }

        isThinking = true
        isListening = false

        recordingJob?.cancel()
        isWebSocketConnected = false
        webSocketClient.close()

        webSocketClient.connect(
            apiKey = apiKey,
            modelName = "models/gemini-2.0-flash-exp",
            voiceName = targetVoiceName,
            systemInstruction = "You are a warm, direct, real-time voice conversational partner. Keep your responses brief, warm, natural, and conversational. Do not output markdown, use direct conversational speech.",
            onAudioDataReceived = { audioBytes ->
                try {
                    if (audioTrack == null) {
                        val sampleRate = 24000
                        val minBufSize = AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        audioTrack = AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufSize.coerceAtLeast(4096),
                            AudioTrack.MODE_STREAM
                        ).apply {
                            play()
                        }
                    }
                    audioTrack?.write(audioBytes, 0, audioBytes.size)
                    isSpeaking = true
                    isThinking = false
                } catch (e: Exception) {
                    android.util.Log.e("GeminiLiveDialog", "Playback error: ${e.message}")
                }
            },
            onTextReceived = { text ->
                val lastItem = transcriptHistory.lastOrNull()
                if (lastItem != null && !lastItem.isUser) {
                    transcriptHistory = transcriptHistory.dropLast(1) + LiveTranscriptItem(
                        isUser = false,
                        text = lastItem.text + text
                    )
                } else {
                    transcriptHistory = transcriptHistory + LiveTranscriptItem(
                        isUser = false,
                        text = text
                    )
                }
            },
            onSessionConfigured = {
                isWebSocketConnected = true
                isThinking = false
                isListening = true

                recordingJob = recordingScope.launch(Dispatchers.IO) {
                    try {
                        val sampleRate = 16000
                        val channelConfig = AudioFormat.CHANNEL_IN_MONO
                        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val recorder = AudioRecord(
                                MediaRecorder.AudioSource.MIC,
                                sampleRate,
                                channelConfig,
                                audioFormat,
                                minBufSize.coerceAtLeast(2048)
                            )
                            audioRecord = recorder
                            recorder.startRecording()

                            val buffer = ByteArray(2048)
                            while (isWebSocketConnected) {
                                val read = recorder.read(buffer, 0, buffer.size)
                                if (read > 0) {
                                    val chunk = buffer.copyOf(read)
                                    webSocketClient.sendAudioChunk(chunk)

                                    var sum = 0f
                                    for (i in 0 until read step 2) {
                                        val sample = ((chunk[i + 1].toInt() shl 8) or (chunk[i].toInt() and 0xFF)).toShort()
                                        sum += Math.abs(sample.toFloat())
                                    }
                                    val rms = sum / (read / 2)
                                    val amplitudeNormalized = (rms / 32768f) * 15f
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        speechAmplitude = speechAmplitude * 0.7f + amplitudeNormalized.coerceIn(0f, 1f) * 0.3f
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GeminiLiveDialog", "Recording error: ${e.message}")
                    }
                }
            },
            onError = { err ->
                android.util.Log.e("GeminiLiveDialog", "WebSocket failed: ${err.message}", err)
                isWebSocketConnected = false
                isThinking = false
                isListening = false
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Real Live Voice Agent offline. Switching to local mode.", Toast.LENGTH_SHORT).show()
                    startListening()
                }
            }
        )
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRealLiveSession()
        } else {
            Toast.makeText(context, "Microphone permission is required for Gemini Live to listen to your voice.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            while (isSpeaking) {
                val base = 0.25f + (sin(System.currentTimeMillis() / 140.0).toFloat() * 0.15f)
                val peak = if (System.currentTimeMillis() % 1000 < 350) 0.35f else 0.02f
                speechAmplitude = (base + peak).coerceIn(0.1f, 0.8f)
                kotlinx.coroutines.delay(35)
            }
        } else {
            speechAmplitude = 0f
        }
    }

    LaunchedEffect(isListening) {
        if (!isListening && !isSpeaking) {
            speechAmplitude = 0f
        }
    }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                // Auto-choose premium network voice if available
                tts?.voices?.find { v -> v.name.lowercase().contains("en-us") && v.name.lowercase().contains("network") }?.let {
                    tts?.voice = it
                }
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isSpeaking = true
                            isListening = false
                            isThinking = false
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isSpeaking = false
                            isListening = true
                            isThinking = false
                            if (!isMuted) {
                                startListening()
                            }
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isSpeaking = false
                            isListening = true
                            isThinking = false
                            if (!isMuted) {
                                startListening()
                            }
                        }
                    }
                })
            }
        }
        ttsEngine = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isCameraActive = true
        } else {
            Toast.makeText(context, "Camera permission is required for Live Video", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            startRealLiveSession()
        } else {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
        onDispose {
            isWebSocketConnected = false
            recordingJob?.cancel()
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {}
            audioRecord = null
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {}
            audioTrack = null
            webSocketClient.close()

            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            ttsEngine?.stop()
        }
    }

    // Auto-scroll when new transcript arrives
    LaunchedEffect(transcriptHistory.size) {
        if (transcriptHistory.isNotEmpty()) {
            listState.animateScrollToItem(transcriptHistory.size - 1)
        }
    }

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFFFFFFFF),
                                0.60f to Color(0xFFFFFFFF),
                                0.85f to Color(0xFFF0F6FE),
                                1.0f to Color(0xFFDCEBFC)
                            )
                        )
                    )
            ) {
                // If Camera is active, show the live Camera viewfinder
                if (isCameraActive) {
                    GeminiLiveCameraView(
                        isFrontCamera = isFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TOP BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Menu Icon
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isCameraActive) Color(0x66000000) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = if (isCameraActive) Color.White else Color(0xFF1F1F1F),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Right action items
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCameraActive) {
                                // Camera flip switch
                                IconButton(
                                    onClick = { isFrontCamera = !isFrontCamera },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Flip camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Subtitles / Voice toggle
                            IconButton(
                                onClick = { showVoiceSelector = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isCameraActive) Color(0x66000000) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Choose Voice",
                                    tint = if (isCameraActive) Color.White else Color(0xFF444746),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 3-dots Menu
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isCameraActive) Color(0x66000000) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = if (isCameraActive) Color.White else Color(0xFF444746),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Choose a voice", fontSize = 15.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF0B57D0))
                                        },
                                        onClick = {
                                            showMenu = false
                                            showVoiceSelector = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Screen Share", fontSize = 15.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.ScreenShare, contentDescription = null, tint = Color(0xFF444746))
                                        },
                                        onClick = {
                                            showMenu = false
                                            showScreenShareSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Minimize to Bubble", fontSize = 15.sp) },
                                        onClick = {
                                            showMenu = false
                                            onMinimizeToBubble()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // CENTER CONTENT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (transcriptHistory.isEmpty() && !isThinking) {
                            // Default Starting View: 4-pointed Gemini star + prompt
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                GeminiLiveOrb(
                                    size = 190.dp,
                                    isActive = !isMuted,
                                    isSpeaking = isSpeaking,
                                    amplitude = speechAmplitude,
                                    onClick = {
                                        if (!isMuted) {
                                            startListening()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                Text(
                                    text = "What would you like\nto explore today?",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 36.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isCameraActive) Color.White else Color(0xFF1F1F1F)
                                )

                                if (isMuted) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFDE8E8),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = "Microphone is muted",
                                            color = Color(0xFFB3261E),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Conversation View (Screenshot 9)
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(transcriptHistory, key = { it.id }) { item ->
                                    if (item.isUser) {
                                        // User speech
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isCameraActive) Color(0xCC000000) else Color(0xFFE9EEF6),
                                                modifier = Modifier.padding(start = 48.dp)
                                            ) {
                                                Text(
                                                    text = item.text,
                                                    fontSize = 16.sp,
                                                    color = if (isCameraActive) Color.White else Color(0xFF1F1F1F),
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Gemini Live AI response
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 24.dp)
                                        ) {
                                            Text(
                                                text = item.text,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Normal,
                                                lineHeight = 30.sp,
                                                color = if (isCameraActive) Color.White else Color(0xFF1F1F1F)
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Response Action icons (Thumbs up, Thumbs down, Share, Copy, More)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        transcriptHistory = transcriptHistory.map {
                                                            if (it.id == item.id) it.copy(isLiked = if (it.isLiked == true) null else true) else it
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isLiked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                                        contentDescription = "Good response",
                                                        tint = if (item.isLiked == true) Color(0xFF0B57D0) else Color(0xFF747775),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        transcriptHistory = transcriptHistory.map {
                                                            if (it.id == item.id) it.copy(isLiked = if (it.isLiked == false) null else false) else it
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isLiked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                                        contentDescription = "Bad response",
                                                        tint = if (item.isLiked == false) Color(0xFFB3261E) else Color(0xFF747775),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val sendIntent = Intent().apply {
                                                            action = Intent.ACTION_SEND
                                                            putExtra(Intent.EXTRA_TEXT, item.text)
                                                            type = "text/plain"
                                                        }
                                                        context.startActivity(Intent.createChooser(sendIntent, "Share response"))
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share",
                                                        tint = Color(0xFF747775),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(item.text))
                                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy",
                                                        tint = Color(0xFF747775),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isThinking) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 24.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.sweepGradient(
                                                                listOf(
                                                                    Color(0xFF91E4FB),
                                                                    Color(0xFF86A8E7),
                                                                    Color(0xFFD1A6FF),
                                                                    Color(0xFF91E4FB)
                                                                )
                                                            )
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Gemini is thinking...",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isCameraActive) Color.White else Color(0xFF0B57D0)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BOTTOM 5-BUTTON FLOATING BAR (Screenshots 2, 3, 6, 7, 8, 9, 11)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Camera Button - Authentic Circular Button
                        Surface(
                            onClick = {
                                if (isCameraActive) {
                                    isCameraActive = false
                                } else {
                                    val hasCam = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCam) {
                                        isCameraActive = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = if (isCameraActive) Color(0xFFD3E3FD) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCameraActive) Color(0xFFA8C7FA) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Camera",
                                    tint = if (isCameraActive) Color(0xFF041E49) else Color(0xFF1F1F1F),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 2. Screen Share / Upload Button - Authentic Circular Button
                        Surface(
                            onClick = {
                                showScreenShareSheet = true
                            },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Screen share / Upload",
                                    tint = Color(0xFF1F1F1F),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 3. Center Glowing Fluid Capsule (Iconic Gemini Live Visualizer)
                        GeminiLiveGlowingCapsule(
                            width = 110.dp,
                            height = 52.dp,
                            isActive = !isMuted,
                            isSpeaking = isSpeaking,
                            amplitude = speechAmplitude,
                            onClick = {
                                // Tap capsule to manually trigger speech / query
                                if (!isMuted) {
                                    startListening()
                                }
                            }
                        )

                        // 4. Microphone Mute / Unmute Button - Authentic Circular Button
                        Surface(
                            onClick = {
                                isMuted = !isMuted
                                if (isMuted) {
                                    speechRecognizer?.stopListening()
                                    ttsEngine?.stop()
                                } else {
                                    startListening()
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = if (isMuted) Color(0xFFFCE8E6) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isMuted) Color(0xFFF9DEDC) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = if (isMuted) Color(0xFFB3261E) else Color(0xFF1F1F1F),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 5. Close / End Button - Authentic Circular Button
                        Surface(
                            onClick = {
                                if (liveSpokenText.isNotBlank()) {
                                    onSpeakToChat(liveSpokenText)
                                }
                                onDismiss()
                            },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "End Live session",
                                    tint = Color(0xFF1F1F1F),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

    // Voice Selection Sheet
    if (showVoiceSelector) {
        GeminiLiveVoiceSelectorSheet(
            selectedVoiceId = currentVoice.id,
            onVoiceSelected = { voice ->
                currentVoice = voice
                speakWithGeminiVoice(voice.previewText, voice)
            },
            onPreviewVoice = { voice ->
                speakWithGeminiVoice(voice.previewText, voice)
            },
            onDismiss = { showVoiceSelector = false }
        )
    }

    // Screen Share Sheet
    if (showScreenShareSheet) {
        GeminiLiveScreenShareSheet(
            onStartScreenShare = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please enable Screen Overlay permission for Gemini Live", Toast.LENGTH_LONG).show()
                } else {
                    val serviceIntent = Intent(context, com.example.gemini.service.GeminiOverlayService::class.java)
                    context.startService(serviceIntent)
                    onDismiss()
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                }
            },
            onDismiss = { showScreenShareSheet = false }
        )
    }
}
