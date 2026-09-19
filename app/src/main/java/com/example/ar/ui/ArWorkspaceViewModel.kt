package com.example.ar.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar.analysis.RoomAnalyzer
import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.ScanningState
import com.example.ar.capability.ArAvailability
import com.example.ar.capability.ArCapabilityChecker
import com.example.ar.session.ArRuntimeState
import com.example.ar.session.ArSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel managing the lifecycle, capability checks, room environment scanning, and runtime state of the AR screen.
 */
class ArWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    val capabilityChecker = ArCapabilityChecker(application.applicationContext)
    val sessionManager = ArSessionManager(capabilityChecker)
    val roomAnalyzer = RoomAnalyzer()

    val runtimeState: StateFlow<ArRuntimeState> = sessionManager.state

    private val _availability = MutableStateFlow<ArAvailability>(ArAvailability.CHECKING)
    val availability: StateFlow<ArAvailability> = _availability.asStateFlow()

    private val _scanResult = MutableStateFlow(RoomScanResult.initial(sessionManager.isDepthSupported()))
    val scanResult: StateFlow<RoomScanResult> = _scanResult.asStateFlow()

    private val _isDebugOverlayVisible = MutableStateFlow(false)
    val isDebugOverlayVisible: StateFlow<Boolean> = _isDebugOverlayVisible.asStateFlow()

    private var analysisJob: Job? = null

    init {
        checkCapability()
    }

    fun toggleDebugOverlay() {
        _isDebugOverlayVisible.value = !_isDebugOverlayVisible.value
    }

    /**
     * Periodically analyzes plane geometry in the background without burdening the render thread.
     */
    fun startScanningLoop() {
        if (analysisJob?.isActive == true) return
        analysisJob = viewModelScope.launch {
            while (isActive) {
                val state = sessionManager.state.value
                when (state) {
                    is ArRuntimeState.Tracking -> {
                        val planes = sessionManager.getDetectedPlanes()
                        val cameraPos = sessionManager.getCameraPosition()
                        val isDepth = sessionManager.isDepthSupported()
                        val result = roomAnalyzer.analyze(
                            planes = planes,
                            cameraPosition = cameraPos,
                            isDepthAvailable = isDepth,
                            isTracking = true
                        )
                        _scanResult.value = result
                    }
                    is ArRuntimeState.TrackingLost -> {
                        val isDepth = sessionManager.isDepthSupported()
                        _scanResult.value = _scanResult.value.copy(
                            state = ScanningState.TRACKING_LOST,
                            guidanceText = "Tracking lost: ${state.reason}",
                            isDepthAvailable = isDepth
                        )
                    }
                    else -> {}
                }
                delay(300)
            }
        }
    }

    fun stopScanningLoop() {
        analysisJob?.cancel()
        analysisJob = null
    }

    fun resetScan() {
        roomAnalyzer.reset()
        _scanResult.value = RoomScanResult.initial(sessionManager.isDepthSupported())
    }

    /**
     * Checks ARCore availability and updates the state.
     */
    fun checkCapability() {
        viewModelScope.launch {
            _availability.value = ArAvailability.CHECKING
            val avail = capabilityChecker.checkArAvailability()
            _availability.value = avail

            if (!capabilityChecker.hasCameraPermission()) {
                // Permission takes priority
                return@launch
            }

            when (avail) {
                ArAvailability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    // SessionManager will be in Unavailable state
                }
                ArAvailability.SUPPORTED_NOT_INSTALLED,
                ArAvailability.SUPPORTED_APK_TOO_OLD -> {
                    // SessionManager will reflect InstallRequired
                }
                ArAvailability.SUPPORTED_INSTALLED -> {
                    // Ready
                }
                else -> {}
            }
        }
    }

    /**
     * Resumes the ARCore session after confirming permissions and unbinding CameraX.
     */
    fun startSession(activity: Activity) {
        viewModelScope.launch {
            if (!capabilityChecker.hasCameraPermission()) {
                return@launch
            }
            sessionManager.resume(activity)
            startScanningLoop()
        }
    }

    /**
     * Pauses the ARCore session and releases the hardware camera.
     */
    fun pauseSession() {
        stopScanningLoop()
        sessionManager.pause()
    }

    /**
     * Closes the session permanently when exiting the screen.
     */
    fun closeSession() {
        stopScanningLoop()
        resetScan()
        sessionManager.close()
    }

    /**
     * Requests installation of Google Play Services for AR.
     */
    fun requestInstall(activity: Activity) {
        val installed = capabilityChecker.requestInstall(activity, userRequested = true)
        if (installed) {
            checkCapability()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanningLoop()
        sessionManager.close()
    }
}
