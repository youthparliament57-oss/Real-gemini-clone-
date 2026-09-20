package com.example.ar.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar.analysis.RoomAnalyzer
import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.ScanningState
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.capability.ArAvailability
import com.example.ar.capability.ArCapabilityChecker
import com.example.ar.session.ArRuntimeState
import com.example.ar.session.ArSessionManager
import com.example.ar.calibration.CalibrationManager
import com.example.ar.calibration.CalibrationMethod
import com.example.ar.calibration.CalibrationResult
import com.example.ar.calibration.CalibrationState
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
    val calibrationManager = CalibrationManager()

    val runtimeState: StateFlow<ArRuntimeState> = sessionManager.state

    private val _availability = MutableStateFlow<ArAvailability>(ArAvailability.CHECKING)
    val availability: StateFlow<ArAvailability> = _availability.asStateFlow()

    private val _scanResult = MutableStateFlow(RoomScanResult.initial(sessionManager.isDepthSupported()))
    val scanResult: StateFlow<RoomScanResult> = _scanResult.asStateFlow()

    private val _isDebugOverlayVisible = MutableStateFlow(false)
    val isDebugOverlayVisible: StateFlow<Boolean> = _isDebugOverlayVisible.asStateFlow()

    private val _calibrationState = MutableStateFlow<CalibrationState>(CalibrationState.Scan)
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    private val _ceilingHeight = MutableStateFlow(CalibrationManager.DEFAULT_CEILING_HEIGHT_METERS)
    val ceilingHeight: StateFlow<Float> = _ceilingHeight.asStateFlow()

    private var cachedAutoResult: CalibrationResult? = null
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
        _calibrationState.value = CalibrationState.Scan
        cachedAutoResult = null
    }

    /**
     * Attempts to run automatic calibration using current scan results.
     */
    fun attemptAutoCalibration() {
        _calibrationState.value = CalibrationState.AutoCalibration
        val result = calibrationManager.attemptAutoCalibration(_scanResult.value, _ceilingHeight.value)
        if (result != null) {
            cachedAutoResult = result
            _calibrationState.value = CalibrationState.AutoCalibrated
        } else {
            // If automatic calibration is insufficient, transition to assisted calibration
            startAssistedCalibration()
        }
    }

    /**
     * Confirms the automatic calibration result.
     */
    fun confirmAutoCalibration() {
        val result = cachedAutoResult
        if (result != null) {
            _calibrationState.value = CalibrationState.Calibrated(result)
        } else {
            _calibrationState.value = CalibrationState.CalibrationFailed("No valid automatic calibration found.")
        }
    }

    /**
     * Starts assisted calibration, preparing candidate corners for review.
     */
    fun startAssistedCalibration() {
        val candidateCorners = _scanResult.value.candidateCorners.map { it.position }
        _calibrationState.value = CalibrationState.AssistedCalibration(
            candidateCorners = candidateCorners,
            confirmedCorners = emptyList()
        )
    }

    /**
     * Confirms a single corner position in assisted calibration.
     * Automatically completes and validates once 4 corners are collected.
     */
    fun confirmAssistedCorner(corner: SpatialVector3) {
        val state = _calibrationState.value
        if (state is CalibrationState.AssistedCalibration) {
            val confirmed = state.confirmedCorners + corner
            if (confirmed.size == 4) {
                val result = calibrationManager.buildCalibrationResult(
                    rawCorners = confirmed,
                    ceilingHeight = _ceilingHeight.value,
                    method = CalibrationMethod.ASSISTED,
                    confidence = 0.8f
                )
                if (result != null) {
                    _calibrationState.value = CalibrationState.Calibrated(result)
                } else {
                    val (_, errorMsg) = calibrationManager.validateCalibration(confirmed, _ceilingHeight.value)
                    _calibrationState.value = CalibrationState.CalibrationFailed(errorMsg ?: "Assisted calibration validation failed.")
                }
            } else {
                _calibrationState.value = state.copy(confirmedCorners = confirmed)
            }
        }
    }

    /**
     * Starts manual four-corner calibration fallback.
     */
    fun startManualCalibration() {
        _calibrationState.value = CalibrationState.ManualCalibration(
            cornerIndex = 0,
            collectedCorners = emptyList()
        )
    }

    /**
     * Captures a physical floor point for the current manual corner index.
     * Validates and completes when the fourth corner is added.
     */
    fun captureManualCorner(position: SpatialVector3) {
        val state = _calibrationState.value
        if (state is CalibrationState.ManualCalibration) {
            val updatedCorners = state.collectedCorners + position
            val nextIndex = state.cornerIndex + 1
            if (nextIndex >= 4) {
                // Completed!
                val result = calibrationManager.buildCalibrationResult(
                    rawCorners = updatedCorners,
                    ceilingHeight = _ceilingHeight.value,
                    method = CalibrationMethod.MANUAL,
                    confidence = 1.0f
                )
                if (result != null) {
                    _calibrationState.value = CalibrationState.Calibrated(result)
                } else {
                    val (_, errorMsg) = calibrationManager.validateCalibration(updatedCorners, _ceilingHeight.value)
                    _calibrationState.value = CalibrationState.CalibrationFailed(errorMsg ?: "Manual calibration validation failed.")
                }
            } else {
                _calibrationState.value = CalibrationState.ManualCalibration(
                    cornerIndex = nextIndex,
                    collectedCorners = updatedCorners
                )
            }
        }
    }

    /**
     * Updates manual ceiling-height input (meters) and dynamically recalculates wall frames if already calibrated.
     */
    fun updateCeilingHeight(meters: Float): Boolean {
        if (meters < CalibrationManager.MIN_CEILING_HEIGHT_METERS || meters > CalibrationManager.MAX_CEILING_HEIGHT_METERS) {
            return false
        }
        _ceilingHeight.value = meters

        val state = _calibrationState.value
        if (state is CalibrationState.Calibrated) {
            val oldResult = state.result
            val newResult = calibrationManager.buildCalibrationResult(
                rawCorners = oldResult.calibratedCornersWorld,
                ceilingHeight = meters,
                method = oldResult.calibrationMethod,
                confidence = oldResult.confidence
            )
            if (newResult != null) {
                _calibrationState.value = CalibrationState.Calibrated(newResult)
            }
        }
        return true
    }

    /**
     * Triggers a retry of the calibration process, returning to Scan state.
     */
    fun retryCalibration() {
        _calibrationState.value = CalibrationState.Scan
        cachedAutoResult = null
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
