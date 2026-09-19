package com.example.ar.session

import android.content.Context
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Ensures strict mutual exclusion between CameraX (used by Gemini Live) and ARCore.
 * Releases CameraX bindings before ARCore acquires the physical camera sensor.
 */
object CameraBridge {
    private const val TAG = "CameraBridge"

    /**
     * Unbinds all active CameraX use-cases from the application process so that ARCore
     * can gain exclusive ownership of the physical camera sensor.
     */
    suspend fun releaseCameraX(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    Log.d(TAG, "Successfully unbound all CameraX instances before starting ARCore.")
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unbind CameraX use-cases cleanly: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.w(TAG, "ProcessCameraProvider error in releaseCameraX: ${e.message}")
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }
}
