package com.example.gemini.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import kotlin.math.sin
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

    var transcriptHistory by remember {
        mutableStateOf(
            listOf<LiveTranscriptItem>()
        )
    }

    val listState = rememberLazyListState()

    // Text To Speech instance
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.getDefault()
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakWithGeminiVoice(text: String, voice: GeminiVoice) {
        ttsEngine?.setPitch(voice.pitch)
        ttsEngine?.setSpeechRate(voice.speechRate)
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gemini_live_tts")
        isSpeaking = true
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

    // Speech Recognizer setup
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startListening() {
        if (isMuted) return
        try {
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
                            // Simulate intelligent Live conversational feedback
                            val response = when {
                                text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("नमस्ते", ignoreCase = true) ->
                                    "I'm doing great, thank you for asking! What can I help you with today?"
                                text.contains("idea", ignoreCase = true) || text.contains("आइडिया", ignoreCase = true) ->
                                    "Certainly! We could brainstorm creative concepts, plan a startup strategy, or draft new stories. Where would you like to begin?"
                                else ->
                                    "I understand! Let's explore this together. I can assist you in analyzing or generating solutions right now."
                            }
                            transcriptHistory = transcriptHistory + LiveTranscriptItem(isUser = false, text = response)
                            speakWithGeminiVoice(response, currentVoice)
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
            // Graceful fallback
        }
    }

    DisposableEffect(Unit) {
        startListening()
        onDispose {
            speechRecognizer?.destroy()
            ttsEngine?.stop()
        }
    }

    // Auto-scroll when new transcript arrives
    LaunchedEffect(transcriptHistory.size) {
        if (transcriptHistory.isNotEmpty()) {
            listState.animateScrollToItem(transcriptHistory.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
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
                        if (transcriptHistory.isEmpty()) {
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
                Toast.makeText(context, "Screen share connected with Gemini Live", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScreenShareSheet = false }
        )
    }
}
