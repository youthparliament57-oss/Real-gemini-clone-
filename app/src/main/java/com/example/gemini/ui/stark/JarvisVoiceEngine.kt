package com.example.gemini.ui.stark

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * State for JARVIS Voice Command System in Stark AR Mode.
 */
data class JarvisVoiceState(
    val isListening: Boolean = false,
    val lastRecognizedText: String = "",
    val jarvisStatus: String = "JARVIS STANDBY // VOICE RECOGNITION READY",
    val jarvisResponse: String = "All holographic AR telemetry systems operational, Sir."
)

/**
 * Parsed action commands triggered by voice commands like:
 * - "Jarvis, spawn reactor"
 * - "Jarvis, scan room"
 * - "Jarvis, maximize all"
 * - "Jarvis, lock all"
 * - "Jarvis, clean room"
 * - "Jarvis, snap to desk"
 */
sealed class JarvisVoiceAction {
    data class SpawnWidget(val type: StarkWidgetType) : JarvisVoiceAction()
    data class SnapAllTo(val preset: RoomPresetCoordinate) : JarvisVoiceAction()
    object MinimizeAll : JarvisVoiceAction()
    object MaximizeAll : JarvisVoiceAction()
    object LockAll : JarvisVoiceAction()
    object UnlockAll : JarvisVoiceAction()
    object ClearRoom : JarvisVoiceAction()
    object ResetRoom : JarvisVoiceAction()
    object ToggleGyro : JarvisVoiceAction()
}

class JarvisVoiceEngine(
    private val context: Context,
    private val onActionTriggered: (JarvisVoiceAction, String) -> Unit,
    private val onStateChanged: (JarvisVoiceState) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var currentState = JarvisVoiceState()

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setPitch(0.95f) // Tony Stark's slightly composed JARVIS AI pitch
                textToSpeech?.setSpeechRate(1.05f)
                isTtsReady = true
            }
        }
    }

    fun speak(phrase: String) {
        if (isTtsReady) {
            textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts")
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            updateState(
                currentState.copy(
                    jarvisStatus = "VOICE INPUT HARDWARE UNAVAILABLE",
                    jarvisResponse = "Speech recognition is not available on this device, Sir."
                )
            )
            return
        }

        try {
            stopListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        updateState(
                            currentState.copy(
                                isListening = true,
                                jarvisStatus = "JARVIS LISTENING... // SPEAK NOW"
                            )
                        )
                    }

                    override fun onBeginningOfSpeech() {
                        updateState(currentState.copy(jarvisStatus = "RECEIVING VOCAL FREQUENCY..."))
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        updateState(currentState.copy(isListening = false, jarvisStatus = "PROCESSING AUDIO COMMAND..."))
                    }

                    override fun onError(error: Int) {
                        updateState(
                            currentState.copy(
                                isListening = false,
                                jarvisStatus = "AUDIO TIMEOUT // TAP MIC TO RETRY"
                            )
                        )
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            processVoiceCommand(text)
                        } else {
                            updateState(currentState.copy(isListening = false, jarvisStatus = "JARVIS STANDBY"))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Jarvis is listening...")
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("JarvisVoiceEngine", "Speech recognizer failure", e)
            updateState(currentState.copy(isListening = false, jarvisStatus = "MICROPHONE BUSY"))
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
        speechRecognizer = null
        updateState(currentState.copy(isListening = false))
    }

    fun processVoiceCommand(rawText: String) {
        val lower = rawText.lowercase(Locale.ROOT)
        var responsePhrase = "Command logged, Sir."
        var triggeredAction: JarvisVoiceAction? = null

        when {
            lower.contains("reactor") || lower.contains("arc") || lower.contains("power") -> {
                triggeredAction = JarvisVoiceAction.SpawnWidget(StarkWidgetType.ARC_REACTOR)
                responsePhrase = "Projecting Arc Reactor MK-85 core into your spatial perimeter, Sir."
            }
            lower.contains("scan") || lower.contains("scanner") || lower.contains("vision") || lower.contains("radar") -> {
                triggeredAction = JarvisVoiceAction.SpawnWidget(StarkWidgetType.VISION_SCANNER)
                responsePhrase = "Deploying Tactical LiDAR vision scanner into room space."
            }
            lower.contains("task") || lower.contains("mission") || lower.contains("directive") -> {
                triggeredAction = JarvisVoiceAction.SpawnWidget(StarkWidgetType.MISSION_MATRIX)
                responsePhrase = "Spawning Stark protocol task matrix."
            }
            lower.contains("clock") || lower.contains("time") || lower.contains("weather") || lower.contains("environment") -> {
                triggeredAction = JarvisVoiceAction.SpawnWidget(StarkWidgetType.QUANTUM_ENVIRONMENT)
                responsePhrase = "Quantum chronometer and atmospheric sensors projected."
            }
            lower.contains("desk") -> {
                triggeredAction = JarvisVoiceAction.SnapAllTo(RoomPresetCoordinate.EAST_DESK)
                responsePhrase = "Aligning holographic array to East Desk coordinate."
            }
            lower.contains("wall") -> {
                triggeredAction = JarvisVoiceAction.SnapAllTo(RoomPresetCoordinate.NORTH_WALL)
                responsePhrase = "Anchoring active holographic telemetry onto North Wall perimeter."
            }
            lower.contains("minimize") || lower.contains("collapse") || lower.contains("hide") -> {
                triggeredAction = JarvisVoiceAction.MinimizeAll
                responsePhrase = "Minimizing active holographic cards to compact HUD state."
            }
            lower.contains("maximize") || lower.contains("expand") || lower.contains("show all") -> {
                triggeredAction = JarvisVoiceAction.MaximizeAll
                responsePhrase = "Maximizing all holographic panels to full telemetry view."
            }
            lower.contains("lock") || lower.contains("pin") || lower.contains("freeze") -> {
                triggeredAction = JarvisVoiceAction.LockAll
                responsePhrase = "Locking all holographic coordinates to room anchors."
            }
            lower.contains("unlock") || lower.contains("float") || lower.contains("free") -> {
                triggeredAction = JarvisVoiceAction.UnlockAll
                responsePhrase = "All widgets unlocked and ready for manual spatial relocation."
            }
            lower.contains("clear") || lower.contains("clean") || lower.contains("remove all") -> {
                triggeredAction = JarvisVoiceAction.ClearRoom
                responsePhrase = "Clearing holographic room space."
            }
            lower.contains("reset") || lower.contains("default") -> {
                triggeredAction = JarvisVoiceAction.ResetRoom
                responsePhrase = "Restoring default Stark Lab laboratory array."
            }
            lower.contains("gyro") || lower.contains("tracking") -> {
                triggeredAction = JarvisVoiceAction.ToggleGyro
                responsePhrase = "Toggling spatial gyroscope sensor matrix."
            }
            else -> {
                responsePhrase = "Understood, Sir. Holographic telemetry is synchronized with '$rawText'."
            }
        }

        speak(responsePhrase)

        val updatedState = currentState.copy(
            isListening = false,
            lastRecognizedText = rawText,
            jarvisStatus = "COMMAND: \"$rawText\"",
            jarvisResponse = responsePhrase
        )
        updateState(updatedState)

        if (triggeredAction != null) {
            onActionTriggered(triggeredAction, responsePhrase)
        }
    }

    private fun updateState(newState: JarvisVoiceState) {
        currentState = newState
        onStateChanged(newState)
    }

    fun release() {
        stopListening()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}

@Composable
fun rememberJarvisVoiceEngine(
    onActionTriggered: (JarvisVoiceAction, String) -> Unit
): Pair<JarvisVoiceState, JarvisVoiceEngine> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(JarvisVoiceState()) }

    val engine = remember {
        JarvisVoiceEngine(
            context = context,
            onActionTriggered = onActionTriggered,
            onStateChanged = { state = it }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.release()
        }
    }

    return Pair(state, engine)
}
