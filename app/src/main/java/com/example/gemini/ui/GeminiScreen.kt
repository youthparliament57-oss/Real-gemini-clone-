package com.example.gemini.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemini.ui.components.GeminiInputBar
import com.example.gemini.ui.components.GeminiLiveDialog
import com.example.gemini.ui.components.GeminiLiveFloatingBubble
import com.example.gemini.ui.components.GeminiLiveNotificationBanner
import com.example.gemini.ui.components.GeminiMessageItem
import com.example.gemini.ui.components.GeminiSidebar
import com.example.gemini.ui.components.GeminiTopAppBar
import com.example.gemini.ui.components.GeminiWelcomeScreen
import com.example.gemini.ui.components.SettingsDialog
import com.example.gemini.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun GeminiScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val isLiveOpen by viewModel.isLiveOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val speakingId by viewModel.currentlySpeakingMessageId.collectAsState()

    var isLiveMinimized by remember { mutableStateOf(false) }
    var isLiveBannerMuted by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Android Speech Recognizer launcher for voice input
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.updateInputText(spokenText)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GeminiSidebar(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSelectSession = { id ->
                    viewModel.selectSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                onTogglePinSession = { s -> viewModel.togglePinSession(s) },
                onOpenSettings = {
                    viewModel.openSettings()
                    scope.launch { drawerState.close() }
                },
                userEmail = "roshanyadavofficial4@gmail.com"
            )
        }
    ) {
        // Authentic White Theme Canvas with soft subtle sky-blue bottom gradient matching the screenshot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFFFFFFF),
                            0.45f to Color(0xFFFFFFFF),
                            0.75f to Color(0xFFF1F6FD),
                            1.0f to Color(0xFFE2EDFC)
                        )
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    GeminiTopAppBar(
                        currentModel = currentModel,
                        onModelSelected = { model -> viewModel.selectModel(model) },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onNewChatClick = {
                            viewModel.startNewChat()
                        },
                        userEmail = "roshanyadavofficial4@gmail.com",
                        modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    )
                },
                bottomBar = {
                    GeminiInputBar(
                        text = inputText,
                        onTextChange = { viewModel.updateInputText(it) },
                        onSend = { viewModel.sendMessage() },
                        onMicClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini…")
                            }
                            speechRecognizerLauncher.launch(intent)
                        },
                        onLiveClick = { viewModel.openLiveMode() },
                        selectedImageUri = selectedImageUri,
                        onImageSelected = { uri -> viewModel.setSelectedImageUri(uri) },
                        modifier = Modifier
                            .imePadding()
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Live with Gemini Notification banner (Screenshot 4, 8)
                        if (isLiveOpen && isLiveMinimized) {
                            GeminiLiveNotificationBanner(
                                isMuted = isLiveBannerMuted,
                                onToggleMute = { isLiveBannerMuted = !isLiveBannerMuted },
                                onEndSession = {
                                    viewModel.closeLiveMode()
                                    isLiveMinimized = false
                                }
                            )
                        }

                        if (messages.isEmpty()) {
                            // Exact Welcome Screen from screenshot with Gemini Star and Hindi Heading
                            GeminiWelcomeScreen(
                                onSuggestionClick = { prompt ->
                                    viewModel.sendMessage(prompt)
                                }
                            )
                        } else {
                            // Chat Message List with Markdown Formatting
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("gemini_messages_list")
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    GeminiMessageItem(
                                        message = msg,
                                        onLikeToggle = { isLiked -> viewModel.toggleLike(msg.id, isLiked) },
                                        onModifyResponse = { option -> viewModel.modifyResponse(msg, option) },
                                        onSpeak = { text -> viewModel.speakMessage(msg.id, text) },
                                        isSpeaking = speakingId == msg.id
                                    )
                                }
                            }
                        }
                    }

                    // Floating Live Bubble in bottom right when minimized
                    if (isLiveOpen && isLiveMinimized) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp, end = 8.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            GeminiLiveFloatingBubble(
                                isMuted = isLiveBannerMuted,
                                isSpeaking = false,
                                onClick = {
                                    isLiveMinimized = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Gemini Live Fullscreen Dialog
    if (isLiveOpen && !isLiveMinimized) {
        GeminiLiveDialog(
            onDismiss = {
                viewModel.closeLiveMode()
                isLiveMinimized = false
            },
            onSpeakToChat = { spokenText ->
                viewModel.sendMessage(spokenText)
                viewModel.closeLiveMode()
                isLiveMinimized = false
            },
            onMinimizeToBubble = {
                isLiveMinimized = true
            }
        )
    }

    // Settings Dialog
    if (isSettingsOpen) {
        SettingsDialog(
            apiKey = customApiKey,
            onSaveApiKey = { viewModel.setCustomApiKey(it) },
            onDismiss = { viewModel.closeSettings() }
        )
    }
}
