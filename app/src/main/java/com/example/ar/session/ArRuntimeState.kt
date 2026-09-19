package com.example.ar.session

/**
 * Clean runtime state model representing the current state of the ARCore subsystem.
 * Exposes UI-safe state without leaking ARCore session or frame objects.
 */
sealed class ArRuntimeState {
    /** Device does not support ARCore. */
    data class Unavailable(val reason: String = "This device does not support ARCore.") : ArRuntimeState()

    /** Google Play Services for AR must be installed or updated. */
    object InstallRequired : ArRuntimeState()

    /** Camera permission is required to run the AR camera session. */
    object PermissionRequired : ArRuntimeState()

    /** ARCore is installed and permissions are granted; ready to start session. */
    object Ready : ArRuntimeState()

    /** ARCore session is running and warming up, waiting for camera tracking. */
    data class Running(val isDepthSupported: Boolean = false) : ArRuntimeState()

    /** ARCore is actively tracking the physical environment in 6DoF. */
    data class Tracking(val isDepthSupported: Boolean = false) : ArRuntimeState()

    /** Tracking is temporarily degraded or lost (e.g. excessive motion, poor lighting). */
    data class TrackingLost(val reason: String = "Tracking lost. Move device slowly.") : ArRuntimeState()

    /** ARCore session is paused and camera hardware is released. */
    object Paused : ArRuntimeState()

    /** An unrecoverable or initialization error occurred. */
    data class Error(val message: String) : ArRuntimeState()
}
