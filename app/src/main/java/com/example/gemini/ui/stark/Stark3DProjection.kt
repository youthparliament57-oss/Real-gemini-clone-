package com.example.gemini.ui.stark

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result of projecting a 3D World Coordinate (X, Y, Z in meters) onto the 2D Camera Screen:
 * - isVisibleInFov: True if the point is in front of the camera and within camera viewport
 * - screenX, screenY: Screen pixel coordinates
 * - perspectiveScale: Perspective scale factor (objects further away look smaller, closer look bigger)
 * - distanceMeters: Real euclidean distance from phone to dashboard
 * - angleOffCenterDegrees: Deviation angle from central camera gaze
 */
data class Stark3DProjectionResult(
    val isVisibleInFov: Boolean,
    val screenX: Float,
    val screenY: Float,
    val perspectiveScale: Float,
    val distanceMeters: Float,
    val angleOffCenterDegrees: Float,
    val directionGuideAngleRad: Float // Direction indicator arrow on edge of screen if outside FoV
)

object StarkSpatialMath {
    // Standard phone camera Field of View in degrees (Portrait mode)
    const val CAMERA_HORIZONTAL_FOV_DEG = 62f
    const val CAMERA_VERTICAL_FOV_DEG = 78f

    /**
     * Projects a 3D world position (Xw, Yw, Zw) to 2D screen coordinates
     * using the phone camera's 6-DoF position and orientation.
     */
    fun projectWorldPointToScreen(
        worldPos: Vector3D,
        cameraPos: Vector3D,
        cameraYawDeg: Float,
        cameraPitchDeg: Float,
        screenWidthPx: Float,
        screenHeightPx: Float,
        baseScale: Float = 1.0f
    ): Stark3DProjectionResult {
        // 1. Relative translation vector from camera to object in world coordinates
        val dx = worldPos.x - cameraPos.x
        val dy = worldPos.y - cameraPos.y
        val dz = worldPos.z - cameraPos.z

        val distance = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.2f)

        // 2. Rotate world vector into camera reference frame
        // Yaw rotation around vertical axis (Y)
        val yawRad = Math.toRadians(cameraYawDeg.toDouble())
        val cosY = cos(-yawRad).toFloat()
        val sinY = sin(-yawRad).toFloat()

        // X and Z after camera Yaw
        val rx1 = dx * cosY - dz * sinY
        val rz1 = dx * sinY + dz * cosY

        // Pitch rotation around horizontal axis (X)
        val pitchRad = Math.toRadians(cameraPitchDeg.toDouble())
        val cosP = cos(-pitchRad).toFloat()
        val sinP = sin(-pitchRad).toFloat()

        // Y and Z after camera Pitch
        val ry2 = dy * cosP - rz1 * sinP
        val rz2 = dy * sinP + rz1 * cosP

        // Camera local coordinates:
        // Local X: right (+), left (-)
        // Local Y: up (+), down (-)
        // Local Z: forward (+) -> MUST BE POSITIVE to be in front of the lens!
        val camX = rx1
        val camY = ry2
        val camZ = rz2

        val isBehindCamera = camZ <= 0.1f

        // Horizontal and vertical angular deviation from camera boresight
        val angleXRad = atan2(camX, camZ)
        val angleYRad = atan2(camY, camZ)

        val angleXDeg = Math.toDegrees(angleXRad.toDouble()).toFloat()
        val angleYDeg = Math.toDegrees(angleYRad.toDouble()).toFloat()

        // Total angular deviation from center gaze
        val totalDeviationDeg = sqrt(angleXDeg * angleXDeg + angleYDeg * angleYDeg)

        // Screen center
        val centerX = screenWidthPx / 2f
        val centerY = screenHeightPx / 2f

        // Camera focal length in pixels based on FoV
        val fovXRad = Math.toRadians((CAMERA_HORIZONTAL_FOV_DEG / 2f).toDouble()).toFloat()
        val fovYRad = Math.toRadians((CAMERA_VERTICAL_FOV_DEG / 2f).toDouble()).toFloat()
        val focalLengthPxX = (screenWidthPx / 2f) / kotlin.math.tan(fovXRad)
        val focalLengthPxY = (screenHeightPx / 2f) / kotlin.math.tan(fovYRad)

        // Perspective projection: screen = center + focal * (camX / camZ)
        val projScreenX = centerX + (focalLengthPxX * (camX / if (camZ > 0.05f) camZ else 0.05f))
        // In screen coordinates, positive Y is DOWN, whereas in 3D camY positive is UP
        val projScreenY = centerY - (focalLengthPxY * (camY / if (camZ > 0.05f) camZ else 0.05f))

        // True 3D perspective scale: 1 meter distance = 1.0 scale factor.
        // Closer than 1m -> larger scale. Farther than 1m -> smaller scale.
        // Clamped gracefully between 0.4x and 1.8x
        val perspectiveFactor = ((1.4f / distance) * baseScale).coerceIn(0.35f, 1.8f)

        // Visibility determination: Must be in front of camera and within screen boundaries with margin
        val margin = 80f
        val isInsideScreenBounds = projScreenX in -margin..(screenWidthPx + margin) &&
                projScreenY in -margin..(screenHeightPx + margin)
        val isVisibleInFov = !isBehindCamera && isInsideScreenBounds

        // Compute off-screen guide arrow direction
        val guideAngle = atan2(projScreenY - centerY, projScreenX - centerX)

        return Stark3DProjectionResult(
            isVisibleInFov = isVisibleInFov,
            screenX = projScreenX,
            screenY = projScreenY,
            perspectiveScale = perspectiveFactor,
            distanceMeters = distance,
            angleOffCenterDegrees = totalDeviationDeg,
            directionGuideAngleRad = guideAngle
        )
    }

    /**
     * Calculates the 3D world coordinate in front of the camera when spawning or moving a widget.
     * Given the phone's current camera position, current yaw, pitch, and requested forward distance in meters.
     */
    fun calculateWorldPointInFrontOfCamera(
        cameraPos: Vector3D,
        yawDeg: Float,
        pitchDeg: Float,
        distanceMeters: Float = 1.6f
    ): Vector3D {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())

        // Direction unit vector pointing forward from camera
        val dirX = (sin(yawRad) * cos(pitchRad)).toFloat()
        val dirY = (sin(pitchRad)).toFloat()
        val dirZ = (cos(yawRad) * cos(pitchRad)).toFloat()

        return Vector3D(
            x = cameraPos.x + dirX * distanceMeters,
            y = cameraPos.y + dirY * distanceMeters,
            z = cameraPos.z + dirZ * distanceMeters
        )
    }
}
