package com.example.ar.session

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.ar.analysis.model.SpatialPoint2D
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.capability.ArCapabilityChecker
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detailed diagnostic model for AR floor hit-testing and corner calibration.
 */
data class HitTestDiagnostic(
    val trackedHorizontalPlanesCount: Int = 0,
    val selectedFloorPlaneId: String? = null,
    val floorPlaneTrackingState: String? = null,
    val floorPolygonVertexCount: Int = 0,
    val floorElevationY: Float? = null,
    val totalHitResultsCount: Int = 0,
    val hitTrackableTypes: List<String> = emptyList(),
    val hitPoses: List<SpatialVector3> = emptyList(),
    val resolutionMethod: String? = null,
    val rejectionReason: String? = null,
    val finalPoint: SpatialVector3? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

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
    private var latestFrame: Frame? = null
    var viewportWidth: Int = 0
        private set
    var viewportHeight: Int = 0
        private set

    private val _state = MutableStateFlow<ArRuntimeState>(ArRuntimeState.Ready)
    val state: StateFlow<ArRuntimeState> = _state.asStateFlow()

    private val _lastHitTestDiagnostic = MutableStateFlow<HitTestDiagnostic?>(null)
    val lastHitTestDiagnostic: StateFlow<HitTestDiagnostic?> = _lastHitTestDiagnostic.asStateFlow()

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
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "ARCore package com.google.ar.core not found on device", e)
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
        } catch (e: Throwable) {
            // Check if root cause or exception itself is NameNotFoundException or missing ARCore package
            var cause: Throwable? = e
            var isMissingPackage = false
            while (cause != null) {
                if (cause is PackageManager.NameNotFoundException ||
                    cause.message?.contains("com.google.ar.core") == true ||
                    cause.message?.contains("Could not load application package metadata") == true
                ) {
                    isMissingPackage = true
                    break
                }
                cause = cause.cause
            }
            if (isMissingPackage) {
                Log.w(TAG, "ARCore package not installed or metadata inaccessible", e)
                _state.value = ArRuntimeState.InstallRequired
                return null
            }

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
        this.viewportWidth = width
        this.viewportHeight = height
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
            latestFrame = frame
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
            latestFrame = null
            _state.value = ArRuntimeState.Ready
        }
    }

    /**
     * Performs a multi-stage, robust hit test on the active ARCore session at screen pixel coordinates (xPx, yPx).
     *
     * Pipeline evaluation stages:
     * 1. Validate ARCore frame, camera tracking state, and detected horizontal floor planes.
     * 2. Identify the primary floor plane elevation baseline.
     * 3. Evaluate direct horizontal plane polygon hit results.
     * 4. Evaluate ARCore depth sensor / point hit results on the floor plane surface with upward normal checks.
     * 5. Evaluate vertical wall hits near the baseboard (within 35cm of floor level) and project down to floor level.
     * 6. Perform geometric ray-plane projection from camera pose to tracked floor elevation for corners outside polygon bounds.
     * 7. Reject invalid hits (high walls, upward-facing rays, points too far from tracked bounds).
     *
     * Detailed diagnostics are recorded in `lastHitTestDiagnostic` and logged to Logcat.
     */
    fun hitTestFloor(xPx: Float, yPx: Float): SpatialVector3? {
        val currentFrame = latestFrame ?: run {
            val diag = HitTestDiagnostic(
                rejectionReason = "ARCore camera frame is not available."
            )
            _lastHitTestDiagnostic.value = diag
            Log.w(TAG, "hitTestFloor rejected: ${diag.rejectionReason}")
            return null
        }

        val camera = currentFrame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            val diag = HitTestDiagnostic(
                rejectionReason = "ARCore tracking is currently ${camera.trackingState.name}. Hold device steady."
            )
            _lastHitTestDiagnostic.value = diag
            Log.w(TAG, "hitTestFloor rejected: ${diag.rejectionReason}")
            return null
        }

        // 1. Gather all tracked horizontal upward-facing planes
        val allPlanes = getDetectedPlanes()
        val trackedHorizontalPlanes = allPlanes.filter {
            it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.trackingState == TrackingState.TRACKING
        }

        val trackedCount = trackedHorizontalPlanes.size
        if (trackedCount == 0) {
            val diag = HitTestDiagnostic(
                trackedHorizontalPlanesCount = 0,
                totalHitResultsCount = 0,
                rejectionReason = "No horizontal floor surfaces are currently tracked. Pan camera over floor."
            )
            _lastHitTestDiagnostic.value = diag
            Log.w(TAG, "hitTestFloor rejected: ${diag.rejectionReason}")
            return null
        }

        // 2. Select the primary floor plane (lowest horizontal plane below camera or lowest detected plane)
        val cameraPose = camera.displayOrientedPose ?: camera.pose
        val camPos = SpatialVector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
        val selectedFloorPlane = run {
            val beneathCam = trackedHorizontalPlanes.filter { it.centerPose.ty() < camPos.y - 0.2f }
            if (beneathCam.isNotEmpty()) {
                beneathCam.minByOrNull { it.centerPose.ty() }!!
            } else {
                trackedHorizontalPlanes.minByOrNull { it.centerPose.ty() }!!
            }
        }

        val floorY = selectedFloorPlane.centerPose.ty()
        val floorPlaneId = "plane_${selectedFloorPlane.hashCode()}"
        val floorTrackingState = selectedFloorPlane.trackingState.name
        val floorPolygonVertexCount = selectedFloorPlane.polygon.limit() / 2
        val floorCenter = SpatialVector3(
            selectedFloorPlane.centerPose.tx(),
            selectedFloorPlane.centerPose.ty(),
            selectedFloorPlane.centerPose.tz()
        )

        // 3. Execute ARCore frame.hitTest(x, y)
        val hits = try {
            currentFrame.hitTest(xPx, yPx)
        } catch (e: Exception) {
            Log.w(TAG, "Exception during frame.hitTest", e)
            emptyList()
        }

        val hitTypes = mutableListOf<String>()
        val hitPoses = mutableListOf<SpatialVector3>()
        for (i in 0 until hits.size) {
            val hit = hits[i]
            val trackable = hit.trackable
            val pose = hit.hitPose
            hitPoses.add(SpatialVector3(pose.tx(), pose.ty(), pose.tz()))
            val typeName = when (trackable) {
                is Plane -> "Plane(${trackable.type}, ${trackable.trackingState})"
                is DepthPoint -> "DepthPoint(${trackable.trackingState})"
                is Point -> "Point(${trackable.trackingState})"
                else -> trackable?.javaClass?.simpleName ?: "Unknown"
            }
            hitTypes.add(typeName)
        }

        // Candidate 1: Direct Horizontal Plane Polygon Hit
        for (i in 0 until hits.size) {
            val hit = hits[i]
            val trackable = hit.trackable
            if (trackable is Plane && trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                if (abs(pose.ty() - floorY) <= 0.22f) {
                    val point = SpatialVector3(pose.tx(), pose.ty(), pose.tz())
                    val diag = HitTestDiagnostic(
                        trackedHorizontalPlanesCount = trackedCount,
                        selectedFloorPlaneId = floorPlaneId,
                        floorPlaneTrackingState = floorTrackingState,
                        floorPolygonVertexCount = floorPolygonVertexCount,
                        floorElevationY = floorY,
                        totalHitResultsCount = hits.size,
                        hitTrackableTypes = hitTypes,
                        hitPoses = hitPoses,
                        resolutionMethod = "Direct Floor Plane Polygon Hit",
                        rejectionReason = null,
                        finalPoint = point
                    )
                    _lastHitTestDiagnostic.value = diag
                    Log.d(TAG, "hitTestFloor SUCCESS: Method=${diag.resolutionMethod}, Point=$point, FloorY=$floorY")
                    return point
                }
            }
        }

        // Candidate 2: ARCore Depth Hit on Floor Surface
        for (i in 0 until hits.size) {
            val hit = hits[i]
            val trackable = hit.trackable
            if (trackable is Point && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                val elevationDiff = abs(pose.ty() - floorY)
                if (elevationDiff <= 0.20f) {
                    val isUpward = if (trackable is DepthPoint) {
                        pose.yAxis[1] > 0.40f
                    } else {
                        true
                    }
                    if (isUpward) {
                        val point = SpatialVector3(pose.tx(), pose.ty(), pose.tz())
                        val diag = HitTestDiagnostic(
                            trackedHorizontalPlanesCount = trackedCount,
                            selectedFloorPlaneId = floorPlaneId,
                            floorPlaneTrackingState = floorTrackingState,
                            floorPolygonVertexCount = floorPolygonVertexCount,
                            floorElevationY = floorY,
                            totalHitResultsCount = hits.size,
                            hitTrackableTypes = hitTypes,
                            hitPoses = hitPoses,
                            resolutionMethod = "ARCore Depth Sensor Floor Measurement",
                            rejectionReason = null,
                            finalPoint = point
                        )
                        _lastHitTestDiagnostic.value = diag
                        Log.d(TAG, "hitTestFloor SUCCESS: Method=${diag.resolutionMethod}, Point=$point, FloorY=$floorY")
                        return point
                    }
                }
            }
        }

        // Candidate 3: Vertical Wall Plane Hit near Baseboard Junction (within 35cm of floor level)
        for (i in 0 until hits.size) {
            val hit = hits[i]
            val trackable = hit.trackable
            if (trackable is Plane && trackable.type == Plane.Type.VERTICAL && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                if (pose.ty() >= floorY - 0.10f && pose.ty() <= floorY + 0.35f) {
                    // Derive the exact wall-floor junction point on the floor plane
                    val point = SpatialVector3(pose.tx(), floorY, pose.tz())
                    val diag = HitTestDiagnostic(
                        trackedHorizontalPlanesCount = trackedCount,
                        selectedFloorPlaneId = floorPlaneId,
                        floorPlaneTrackingState = floorTrackingState,
                        floorPolygonVertexCount = floorPolygonVertexCount,
                        floorElevationY = floorY,
                        totalHitResultsCount = hits.size,
                        hitTrackableTypes = hitTypes,
                        hitPoses = hitPoses,
                        resolutionMethod = "Wall-Floor Baseboard Intersection",
                        rejectionReason = null,
                        finalPoint = point
                    )
                    _lastHitTestDiagnostic.value = diag
                    Log.d(TAG, "hitTestFloor SUCCESS: Method=${diag.resolutionMethod}, Point=$point, FloorY=$floorY")
                    return point
                }
            }
        }

        // Candidate 4: Geometric Ray-Floor Plane Projection (for corners outside current polygon bounds)
        // Camera ray forward vector in world coordinates:
        val rayDir = SpatialVector3(
            -cameraPose.zAxis[0],
            -cameraPose.zAxis[1],
            -cameraPose.zAxis[2]
        ).normalized()

        // Check if ray is blocked by a high vertical wall closer than the floor
        if (rayDir.y < -0.05f) {
            val t = (floorY - camPos.y) / rayDir.y
            
            val blockingWallHit = hits.firstOrNull { hit ->
                val trk = hit.trackable
                trk is Plane && trk.type == Plane.Type.VERTICAL && hit.hitPose.ty() > floorY + 0.35f
            }

            if (blockingWallHit != null && blockingWallHit.distance < (t - 0.25f)) {
                val rejection = "Ray hit vertical wall (${String.format("%.2f", blockingWallHit.hitPose.ty())}m) above floor. Aim at floor or baseboard."
                val diag = HitTestDiagnostic(
                    trackedHorizontalPlanesCount = trackedCount,
                    selectedFloorPlaneId = floorPlaneId,
                    floorPlaneTrackingState = floorTrackingState,
                    floorPolygonVertexCount = floorPolygonVertexCount,
                    floorElevationY = floorY,
                    totalHitResultsCount = hits.size,
                    hitTrackableTypes = hitTypes,
                    hitPoses = hitPoses,
                    rejectionReason = rejection
                )
                _lastHitTestDiagnostic.value = diag
                Log.w(TAG, "hitTestFloor REJECTED: $rejection")
                return null
            }

            if (t in 0.3f..8.0f) {
                val projectedPoint = camPos + (rayDir * t)
                val distToCenter = SpatialPoint2D(projectedPoint.x, projectedPoint.z)
                    .distanceTo(SpatialPoint2D(floorCenter.x, floorCenter.z))
                val maxAllowedRadius = max(4.0f, sqrt(selectedFloorPlane.extentX * selectedFloorPlane.extentX + selectedFloorPlane.extentZ * selectedFloorPlane.extentZ) + 2.5f)

                if (distToCenter <= maxAllowedRadius) {
                    val diag = HitTestDiagnostic(
                        trackedHorizontalPlanesCount = trackedCount,
                        selectedFloorPlaneId = floorPlaneId,
                        floorPlaneTrackingState = floorTrackingState,
                        floorPolygonVertexCount = floorPolygonVertexCount,
                        floorElevationY = floorY,
                        totalHitResultsCount = hits.size,
                        hitTrackableTypes = hitTypes,
                        hitPoses = hitPoses,
                        resolutionMethod = "Geometric Ray-Floor Projection (dist=${String.format("%.2f", t)}m)",
                        rejectionReason = null,
                        finalPoint = projectedPoint
                    )
                    _lastHitTestDiagnostic.value = diag
                    Log.d(TAG, "hitTestFloor SUCCESS: Method=${diag.resolutionMethod}, Point=$projectedPoint, FloorY=$floorY")
                    return projectedPoint
                } else {
                    val rejection = "Aim point (${String.format("%.1f", distToCenter)}m) is outside tracked floor region."
                    val diag = HitTestDiagnostic(
                        trackedHorizontalPlanesCount = trackedCount,
                        selectedFloorPlaneId = floorPlaneId,
                        floorPlaneTrackingState = floorTrackingState,
                        floorPolygonVertexCount = floorPolygonVertexCount,
                        floorElevationY = floorY,
                        totalHitResultsCount = hits.size,
                        hitTrackableTypes = hitTypes,
                        hitPoses = hitPoses,
                        rejectionReason = rejection
                    )
                    _lastHitTestDiagnostic.value = diag
                    Log.w(TAG, "hitTestFloor REJECTED: $rejection")
                    return null
                }
            } else {
                val rejection = "Aim ray distance (${String.format("%.1f", t)}m) is out of valid range [0.3m, 8.0m]."
                val diag = HitTestDiagnostic(
                    trackedHorizontalPlanesCount = trackedCount,
                    selectedFloorPlaneId = floorPlaneId,
                    floorPlaneTrackingState = floorTrackingState,
                    floorPolygonVertexCount = floorPolygonVertexCount,
                    floorElevationY = floorY,
                    totalHitResultsCount = hits.size,
                    hitTrackableTypes = hitTypes,
                    hitPoses = hitPoses,
                    rejectionReason = rejection
                )
                _lastHitTestDiagnostic.value = diag
                Log.w(TAG, "hitTestFloor REJECTED: $rejection")
                return null
            }
        } else {
            val rejection = "Reticle is pointing horizontally or upward (Y dir: ${String.format("%.2f", rayDir.y)}). Point camera down at floor."
            val diag = HitTestDiagnostic(
                trackedHorizontalPlanesCount = trackedCount,
                selectedFloorPlaneId = floorPlaneId,
                floorPlaneTrackingState = floorTrackingState,
                floorPolygonVertexCount = floorPolygonVertexCount,
                floorElevationY = floorY,
                totalHitResultsCount = hits.size,
                hitTrackableTypes = hitTypes,
                hitPoses = hitPoses,
                rejectionReason = rejection
            )
            _lastHitTestDiagnostic.value = diag
            Log.w(TAG, "hitTestFloor REJECTED: $rejection")
            return null
        }
    }
}

