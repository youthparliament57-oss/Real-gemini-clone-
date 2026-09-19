package com.example.gemini.ui.stark

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Vector representation for world space coordinates in meters.
 * X: Left (-X) to Right (+X)
 * Y: Up (+Y) to Down (-Y)
 * Z: Forward (+Z) to Backward (-Z)
 */
data class Vector3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    fun distanceTo(other: Vector3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

/**
 * Complete 6-Degrees-of-Freedom device tracking state:
 * - Position (X, Y, Z) in meters derived from accelerometer & linear acceleration
 * - Orientation (Yaw/Azimuth, Pitch, Roll) in degrees from Rotation Vector / Gyroscope
 * - Speed (m/s) in 3D space
 */
data class Stark6DoFState(
    val cameraPos: Vector3D = Vector3D(0f, 0f, 0f),
    val azimuthDegrees: Float = 0f,  // 0..360 (Yaw)
    val pitchDegrees: Float = 0f,    // -90..+90 (Pitch)
    val rollDegrees: Float = 0f,     // -180..+180 (Roll)
    val speedMetersPerSec: Float = 0f,
    val rotationMatrix: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f },
    val isTrackingActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Stark6DoFState) return false
        return cameraPos == other.cameraPos &&
                azimuthDegrees == other.azimuthDegrees &&
                pitchDegrees == other.pitchDegrees &&
                rollDegrees == other.rollDegrees &&
                speedMetersPerSec == other.speedMetersPerSec &&
                isTrackingActive == other.isTrackingActive
    }

    override fun hashCode(): Int {
        var result = cameraPos.hashCode()
        result = 31 * result + azimuthDegrees.hashCode()
        result = 31 * result + pitchDegrees.hashCode()
        result = 31 * result + rollDegrees.hashCode()
        result = 31 * result + speedMetersPerSec.hashCode()
        result = 31 * result + isTrackingActive.hashCode()
        return result
    }
}

/**
 * Real-time 6-DoF Sensor Fusion hook combining:
 * 1. TYPE_ROTATION_VECTOR (Gyroscope + Accelerometer + Magnetometer fusion for drift-free orientation)
 * 2. TYPE_LINEAR_ACCELERATION (Phone acceleration without gravity to track speed and 3D translation)
 */
@Composable
fun rememberStark6DoFSensor(): Pair<Stark6DoFState, () -> Unit> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Stark6DoFState()) }

    var currentCameraX by remember { mutableStateOf(0f) }
    var currentCameraY by remember { mutableStateOf(0f) }
    var currentCameraZ by remember { mutableStateOf(0f) }
    var velocityX by remember { mutableStateOf(0f) }
    var velocityY by remember { mutableStateOf(0f) }
    var velocityZ by remember { mutableStateOf(0f) }
    var lastTimestampNs by remember { mutableStateOf(0L) }

    val resetOrigin = remember {
        {
            currentCameraX = 0f
            currentCameraY = 0f
            currentCameraZ = 0f
            velocityX = 0f
            velocityY = 0f
            velocityZ = 0f
            state = state.copy(cameraPos = Vector3D(0f, 0f, 0f), speedMetersPerSec = 0f)
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val linearAccelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || rotationSensor == null) {
            state = state.copy(isTrackingActive = false)
            return@DisposableEffect onDispose {}
        }

        val rotMatrix = FloatArray(16)
        val outRotMatrix = FloatArray(16)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        // Remap coordinates for camera pointing in portrait mode
                        SensorManager.remapCoordinateSystem(
                            rotMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            outRotMatrix
                        )
                        SensorManager.getOrientation(outRotMatrix, orientationAngles)

                        val yawDeg = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
                        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        state = state.copy(
                            azimuthDegrees = yawDeg,
                            pitchDegrees = pitchDeg,
                            rollDegrees = rollDeg,
                            rotationMatrix = rotMatrix.clone(),
                            isTrackingActive = true
                        )
                    }

                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        val currentTimestamp = event.timestamp
                        if (lastTimestampNs != 0L) {
                            val dt = (currentTimestamp - lastTimestampNs) * 1e-9f // seconds
                            if (dt in 0.001f..0.2f) {
                                // Raw acceleration in device coordinates
                                val ax = event.values[0]
                                val ay = event.values[1]
                                val az = event.values[2]

                                // Deadzone filter for human hand trembling (< 0.12 m/s^2)
                                val filtAx = if (abs(ax) > 0.12f) ax else 0f
                                val filtAy = if (abs(ay) > 0.12f) ay else 0f
                                val filtAz = if (abs(az) > 0.12f) az else 0f

                                // Transform acceleration from device frame to world frame using yaw
                                val yawRad = Math.toRadians(state.azimuthDegrees.toDouble())
                                val cosY = cos(yawRad).toFloat()
                                val sinY = sin(yawRad).toFloat()

                                val worldAx = filtAx * cosY - filtAz * sinY
                                val worldAy = filtAy
                                val worldAz = filtAx * sinY + filtAz * cosY

                                // Integrate acceleration into velocity with damping/friction
                                val friction = 0.92f
                                velocityX = (velocityX + worldAx * dt) * friction
                                velocityY = (velocityY + worldAy * dt) * friction
                                velocityZ = (velocityZ + worldAz * dt) * friction

                                val speed = sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ)

                                // Integrate velocity into 3D position
                                currentCameraX += velocityX * dt
                                currentCameraY += velocityY * dt
                                currentCameraZ += velocityZ * dt

                                state = state.copy(
                                    cameraPos = Vector3D(currentCameraX, currentCameraY, currentCameraZ),
                                    speedMetersPerSec = speed
                                )
                            }
                        }
                        lastTimestampNs = currentTimestamp
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        linearAccelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return Pair(state, resetOrigin)
}
