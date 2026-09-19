package com.example.gemini.ui.stark

import android.app.Activity
import android.content.Context
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
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
import kotlin.math.sqrt

/**
 * Real-Time ARCore Spatial SLAM Engine state.
 * Bridges Google ARCore visual-inertial odometry, plane detection, anchors and Pose
 * to our Stark HUD and Jetpack Compose 3D UI.
 */
data class StarkArCoreState(
    val isArCoreSupported: Boolean = false,
    val isTrackingActive: Boolean = false,
    val trackingFailureReason: String? = null,
    val detectedPlanesCount: Int = 0,
    val cameraPoseTranslation: Vector3D = Vector3D(0f, 0f, 0f),
    val cameraPoseRotation: FloatArray = FloatArray(4) { if (it == 3) 1f else 0f },
    val lightIntensity: Float = 1.0f,
    val trackingModeDescription: String = "INITIALIZING SLAM"
)

/**
 * Controller to interact with the underlying ARCore Session:
 * - Create anchor from camera pose or screen tap hit-test
 * - Re-anchor
 * - Project 3D anchor pose to 2D screen coordinate using ARCore view and projection matrices
 */
class StarkArCoreController(
    val sessionProvider: () -> Session?
) {
    /**
     * Hit tests a 2D screen coordinate against detected physical surfaces (planes).
     * If hit, creates an Anchor attached to the physical floor/desk/wall.
     */
    fun hitTestScreenPoint(xPx: Float, yPx: Float, frame: Frame?): Anchor? {
        val s = sessionProvider() ?: return null
        val f = frame ?: return null
        return try {
            val hits = f.hitTest(xPx, yPx)
            val validHit = hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
            } ?: hits.firstOrNull()

            validHit?.createAnchor()
        } catch (e: Exception) {
            Log.e("StarkArCore", "Hit test error", e)
            null
        }
    }

    /**
     * Creates a floating 3D spatial anchor at a specific distance directly in front of the camera pose.
     */
    fun createAnchorInFrontOfCamera(distanceMeters: Float = 1.5f, frame: Frame?): Anchor? {
        val s = sessionProvider() ?: return null
        val f = frame ?: return null
        return try {
            val cameraPose = f.camera.pose
            // In ARCore camera coordinate system:
            // -Z is forward in front of camera
            // +Y is up
            // +X is right
            val forwardInCameraSpace = Pose.makeTranslation(0f, 0f, -distanceMeters)
            val worldPose = cameraPose.compose(forwardInCameraSpace)
            s.createAnchor(worldPose)
        } catch (e: Exception) {
            Log.e("StarkArCore", "Create camera front anchor error", e)
            null
        }
    }
}

/**
 * Composable that manages ARCore Session lifecycle, optical tracking,
 * surface plane detection, and pose matrix computation.
 */
@Composable
fun rememberStarkArCoreSession(
    onFrameUpdate: (Frame, FloatArray, FloatArray) -> Unit = { _, _, _ -> }
): Pair<StarkArCoreState, StarkArCoreController> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var arCoreState by remember { mutableStateOf(StarkArCoreState()) }
    var sessionRef by remember { mutableStateOf<Session?>(null) }

    val controller = remember {
        StarkArCoreController(sessionProvider = { sessionRef })
    }

    // View & Projection matrices (4x4 OpenGL format)
    val viewMatrix = remember { FloatArray(16) }
    val projMatrix = remember { FloatArray(16) }

    DisposableEffect(lifecycleOwner) {
        var arSession: Session? = null
        val mainHandler = Handler(Looper.getMainLooper())

        val availability = ArCoreApk.getInstance().checkAvailability(context)
        val isSupported = availability.isSupported

        if (isSupported) {
            try {
                // Initialize ARCore session
                arSession = Session(context)
                val config = Config(arSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    focusMode = Config.FocusMode.AUTO
                    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                }
                arSession.configure(config)
                sessionRef = arSession

                arCoreState = arCoreState.copy(
                    isArCoreSupported = true,
                    trackingModeDescription = "ARCORE SLAM ACTIVE"
                )
            } catch (e: Exception) {
                Log.w("StarkArCore", "ARCore Session init failed, falling back to 6-DoF sensor fusion", e)
                arCoreState = arCoreState.copy(
                    isArCoreSupported = false,
                    trackingModeDescription = "6-DoF SENSOR FUSION ACTIVE"
                )
            }
        } else {
            arCoreState = arCoreState.copy(
                isArCoreSupported = false,
                trackingModeDescription = "6-DoF HARDWARE SENSORS ACTIVE"
            )
        }

        // Frame polling runner for ARCore updates when session is running
        var isRunning = true
        val frameRunnable = object : Runnable {
            override fun run() {
                val s = sessionRef
                if (s != null && isRunning) {
                    try {
                        val frame = s.update()
                        val camera = frame.camera

                        val isTracking = camera.trackingState == TrackingState.TRACKING
                        val failReason = if (!isTracking) {
                            when (camera.trackingFailureReason) {
                                TrackingFailureReason.INSUFFICIENT_LIGHT -> "LOW LIGHT ENVIRONMENT"
                                TrackingFailureReason.EXCESSIVE_MOTION -> "MOVE PHONE SLOWER"
                                TrackingFailureReason.INSUFFICIENT_FEATURES -> "LOOK AT TEXTURED SURFACE"
                                TrackingFailureReason.BAD_STATE -> "CALIBRATING SLAM"
                                else -> "CALIBRATING OPTICAL FLOW"
                            }
                        } else null

                        camera.getViewMatrix(viewMatrix, 0)
                        camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

                        val pose = camera.pose
                        val tx = pose.tx()
                        val ty = pose.ty()
                        val tz = pose.tz()

                        val planeCount = s.getAllTrackables(Plane::class.java).count {
                            it.trackingState == TrackingState.TRACKING
                        }

                        val lightEst = frame.lightEstimate
                        val light = if (lightEst.state == com.google.ar.core.LightEstimate.State.VALID) {
                            lightEst.pixelIntensity
                        } else 1.0f

                        arCoreState = arCoreState.copy(
                            isTrackingActive = isTracking,
                            trackingFailureReason = failReason,
                            detectedPlanesCount = planeCount,
                            cameraPoseTranslation = Vector3D(tx, ty, tz),
                            cameraPoseRotation = floatArrayOf(pose.qx(), pose.qy(), pose.qz(), pose.qw()),
                            lightIntensity = light,
                            trackingModeDescription = if (isTracking) {
                                if (planeCount > 0) "SURFACE SLAM LOCKED ($planeCount PLANES)" else "OPTICAL ODOMETRY TRACKING"
                            } else (failReason ?: "SEARCHING ENVIRONMENT")
                        )

                        onFrameUpdate(frame, viewMatrix, projMatrix)
                    } catch (e: Exception) {
                        // CameraNotAvailable or session paused
                    }
                }

                if (isRunning) {
                    mainHandler.postDelayed(this, 33) // ~30 FPS frame tracking
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        sessionRef?.resume()
                        isRunning = true
                        mainHandler.post(frameRunnable)
                    } catch (e: Exception) {
                        Log.e("StarkArCore", "Session resume error", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isRunning = false
                    mainHandler.removeCallbacks(frameRunnable)
                    try {
                        sessionRef?.pause()
                    } catch (e: Exception) {
                        Log.e("StarkArCore", "Session pause error", e)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    isRunning = false
                    mainHandler.removeCallbacks(frameRunnable)
                    try {
                        sessionRef?.close()
                        sessionRef = null
                    } catch (e: Exception) {
                        Log.e("StarkArCore", "Session close error", e)
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            isRunning = false
            mainHandler.removeCallbacks(frameRunnable)
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                sessionRef?.close()
                sessionRef = null
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    return Pair(arCoreState, controller)
}

/**
 * Mathematically projects an ARCore Anchor Pose or World Coordinate to 2D Screen Space
 * using ARCore's View and Projection matrices:
 *
 * Screen = Viewport * ProjectionMatrix * ViewMatrix * [X, Y, Z, 1]
 */
object StarkArCoreMath {
    fun projectAnchorPoseToScreen(
        pose: Pose,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        screenWidthPx: Float,
        screenHeightPx: Float,
        baseScale: Float = 1.0f
    ): Stark3DProjectionResult {
        // Anchor world position
        val worldPoint = floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1.0f)

        // Point in camera space: CameraPoint = ViewMatrix * WorldPoint
        val cameraPoint = FloatArray(4)
        Matrix.multiplyMV(cameraPoint, 0, viewMatrix, 0, worldPoint, 0)

        // camZ is negative in standard OpenGL camera coordinates (points into the screen)
        // distance is euclidean norm in camera space
        val distance = sqrt(
            cameraPoint[0] * cameraPoint[0] +
                    cameraPoint[1] * cameraPoint[1] +
                    cameraPoint[2] * cameraPoint[2]
        ).coerceAtLeast(0.1f)

        val isBehindCamera = cameraPoint[2] >= -0.05f

        // Clip space: ClipPoint = ProjMatrix * CameraPoint
        val clipPoint = FloatArray(4)
        Matrix.multiplyMV(clipPoint, 0, projMatrix, 0, cameraPoint, 0)

        // Normalized Device Coordinates (NDC) in range [-1, 1]
        val w = if (clipPoint[3] != 0f) clipPoint[3] else 1.0f
        val ndcX = clipPoint[0] / w
        val ndcY = clipPoint[1] / w

        // Convert NDC to screen pixel coordinates
        val screenX = ((ndcX + 1.0f) * 0.5f) * screenWidthPx
        // In screen space, top is 0, so invert Y
        val screenY = ((1.0f - ndcY) * 0.5f) * screenHeightPx

        val centerX = screenWidthPx / 2f
        val centerY = screenHeightPx / 2f

        // True 3D perspective scaling factor
        val perspectiveFactor = ((1.4f / distance) * baseScale).coerceIn(0.35f, 1.8f)

        val margin = 80f
        val isInsideScreenBounds = screenX in -margin..(screenWidthPx + margin) &&
                screenY in -margin..(screenHeightPx + margin)
        val isVisibleInFov = !isBehindCamera && isInsideScreenBounds

        val guideAngle = kotlin.math.atan2(screenY - centerY, screenX - centerX)

        return Stark3DProjectionResult(
            isVisibleInFov = isVisibleInFov,
            screenX = screenX,
            screenY = screenY,
            perspectiveScale = perspectiveFactor,
            distanceMeters = distance,
            angleOffCenterDegrees = 0f,
            directionGuideAngleRad = guideAngle
        )
    }
}
