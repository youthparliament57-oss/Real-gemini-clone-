package com.example.ar.session

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.ar.capability.ArCapabilityChecker
import com.example.ar.analysis.model.SpatialVector3
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the ARCore Session lifecycle, hardware camera ownership, and configuration.
 * Adheres strictly to the official Google ARCore lifecycle and thread safety patterns.
 */
class ArSessionManager(
    private val capabilityChecker: ArCapabilityChecker
) {
    companion object {
        private const val TAG = "ArSessionManager"
    }

    private var session: Session? = null
    private var isDepthSupported: Boolean = false
    private var latestCameraPose: Pose? = null

    private val _state = MutableStateFlow<ArRuntimeState>(ArRuntimeState.Ready)
    val state: StateFlow<ArRuntimeState> = _state.asStateFlow()

    fun getSession(): Session? = session

    /**
     * Returns all currently detected planes from the ARCore session.
     */
    fun getDetectedPlanes(): List<Plane> {
        return session?.getAllTrackables(Plane::class.java)?.toList() ?: emptyList()
    }

    /**
     * Returns the latest camera position in world coordinates, if available.
     */
    fun getCameraPosition(): SpatialVector3? {
        val translation = latestCameraPose?.translation ?: return null
        return if (translation.size >= 3) {
            SpatialVector3(translation[0], translation[1], translation[2])
        } else {
            null
        }
    }

    fun isDepthSupported(): Boolean = isDepthSupported

    /**
     * Creates and configures a new ARCore Session if not already initialized.
     */
    fun create(context: Context): Session? {
        if (session != null) return session

        if (!capabilityChecker.hasCameraPermission()) {
            _state.value = ArRuntimeState.PermissionRequired
            return null
        }

        try {
            val newSession = Session(context)
            session = newSession
            configure(newSession)
            _state.value = ArRuntimeState.Ready
            return newSession
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.w(TAG, "ARCore not installed", e)
            _state.value = ArRuntimeState.InstallRequired
            return null
        } catch (e: UnavailableApkTooOldException) {
            Log.w(TAG, "ARCore APK too old", e)
            _state.value = ArRuntimeState.InstallRequired
            return null
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "Device SDK too old for ARCore", e)
            _state.value = ArRuntimeState.Unavailable("Android OS update required for AR.")
            return null
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible with ARCore", e)
            _state.value = ArRuntimeState.Unavailable("This device is not compatible with ARCore.")
            return null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception creating AR session", e)
            _state.value = ArRuntimeState.PermissionRequired
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating AR session", e)
            _state.value = ArRuntimeState.Error("Failed to initialize AR session: ${e.message}")
            return null
        }
    }

    /**
     * Configures the active session with Phase 1 parameters:
     * - Horizontal and Vertical plane finding
     * - Automatic autofocus
     * - Automatic depth when supported
     */
    fun configure(targetSession: Session? = session): Boolean {
        val activeSession = targetSession ?: return false
        return try {
            val config = Config(activeSession).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                focusMode = Config.FocusMode.AUTO

                isDepthSupported = activeSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                depthMode = if (isDepthSupported) {
                    Log.d(TAG, "Enabling AUTOMATIC depth mode on ARCore session.")
                    Config.DepthMode.AUTOMATIC
                } else {
                    Log.d(TAG, "Depth mode unsupported on device; falling back to DISABLED.")
                    Config.DepthMode.DISABLED
                }
            }
            activeSession.configure(config)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring ARCore session", e)
            false
        }
    }

    /**
     * Sets display geometry for the ARCore session on view resize or orientation change.
     */
    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        try {
            session?.setDisplayGeometry(rotation, width, height)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set display geometry", e)
        }
    }

    /**
     * Binds the OpenGL external camera texture ID to ARCore.
     */
    fun setCameraTextureNames(textureIds: IntArray) {
        try {
            session?.setCameraTextureNames(textureIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set camera texture names", e)
        }
    }

    /**
     * Resumes the ARCore session and locks hardware camera ownership.
     */
    suspend fun resume(context: Context): ArRuntimeState {
        // Ensure CameraX bindings are safely released first
        CameraBridge.releaseCameraX(context)

        val currentSession = session ?: create(context) ?: return _state.value

        return try {
            currentSession.resume()
            val runningState = ArRuntimeState.Running(isDepthSupported = isDepthSupported)
            _state.value = runningState
            runningState
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available to ARCore", e)
            val err = ArRuntimeState.Error("Camera is currently in use or unavailable.")
            _state.value = err
            err
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission missing on resume", e)
            val perm = ArRuntimeState.PermissionRequired
            _state.value = perm
            perm
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume AR session", e)
            val err = ArRuntimeState.Error("Failed to resume AR: ${e.message}")
            _state.value = err
            err
        }
    }

    /**
     * Updates the ARCore frame synchronously on the GL render thread.
     */
    fun updateFrame(): Frame? {
        val currentSession = session ?: return null
        return try {
            val frame = currentSession.update()
            val camera = frame.camera
            latestCameraPose = camera.pose
            when (camera.trackingState) {
                TrackingState.TRACKING -> {
                    _state.value = ArRuntimeState.Tracking(isDepthSupported = isDepthSupported)
                }
                TrackingState.PAUSED -> {
                    val reasonText = when (camera.trackingFailureReason) {
                        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Too dark. Move to a well-lit area."
                        TrackingFailureReason.EXCESSIVE_MOTION -> "Moving too fast. Hold device steady."
                        TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point camera at a textured surface."
                        TrackingFailureReason.BAD_STATE -> "Tracking resetting. Hold steady."
                        else -> "Scanning environment..."
                    }
                    _state.value = ArRuntimeState.TrackingLost(reasonText)
                }
                TrackingState.STOPPED -> {
                    _state.value = ArRuntimeState.TrackingLost("AR tracking stopped.")
                }
                else -> {}
            }
            frame
        } catch (e: Exception) {
            Log.w(TAG, "Exception during session.update()", e)
            null
        }
    }

    /**
     * Pauses the ARCore session and releases physical camera hardware immediately.
     */
    fun pause(): ArRuntimeState {
        try {
            session?.pause()
            Log.d(TAG, "ARCore session paused; camera hardware released.")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing AR session", e)
        }
        val pausedState = ArRuntimeState.Paused
        _state.value = pausedState
        return pausedState
    }

    /**
     * Closes the ARCore session permanently and frees all associated native resources.
     */
    fun close() {
        try {
            session?.close()
            Log.d(TAG, "ARCore session closed.")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing AR session", e)
        } finally {
            session = null
            latestCameraPose = null
            _state.value = ArRuntimeState.Ready
        }
    }
}
