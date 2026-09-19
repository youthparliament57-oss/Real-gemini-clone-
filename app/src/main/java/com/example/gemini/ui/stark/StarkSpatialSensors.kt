package com.example.gemini.ui.stark

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI

/**
 * State representing device orientation (Yaw, Pitch, Roll) in degrees
 * for spatial anchoring of AR holographic widgets in the room.
 */
data class StarkSpatialOrientation(
    val azimuthDegrees: Float = 0f,  // 0 to 360 (compass / yaw)
    val pitchDegrees: Float = 0f,    // -90 to +90 (looking up / down)
    val rollDegrees: Float = 0f,     // -180 to +180 (tilt)
    val isTrackingActive: Boolean = false
)

/**
 * Custom Compose hook that listens to the phone's Rotation Vector sensor (Gyroscope + Accelerometer + Magnetometer fusion).
 * When the user moves or rotates the phone in the room, this provides real-time angular drift to lock panels in physical space.
 */
@Composable
fun rememberStarkSpatialSensor(): StarkSpatialOrientation {
    val context = LocalContext.current
    var orientation by remember { mutableStateOf(StarkSpatialOrientation()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        if (sensorManager == null || rotationSensor == null) {
            orientation = orientation.copy(isTrackingActive = false)
            return@DisposableEffect onDispose {}
        }

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val azimuth = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
                    val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    orientation = StarkSpatialOrientation(
                        azimuthDegrees = azimuth,
                        pitchDegrees = pitch,
                        rollDegrees = roll,
                        isTrackingActive = true
                    )
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    orientation = StarkSpatialOrientation(
                        azimuthDegrees = (event.values[0] + 360f) % 360f,
                        pitchDegrees = event.values[1],
                        rollDegrees = event.values[2],
                        isTrackingActive = true
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }

    }

    return orientation
}
