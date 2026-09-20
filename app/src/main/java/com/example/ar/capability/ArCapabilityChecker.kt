package com.example.ar.capability

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsulates device-level capability checks for Google ARCore and hardware sensors.
 */
class ArCapabilityChecker(private val context: Context) {

    companion object {
        private const val TAG = "ArCapabilityChecker"
    }

    /**
     * Checks whether the camera permission is currently granted.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Determines whether this device supports ARCore and whether the required APK is installed.
     */
    suspend fun checkArAvailability(): ArAvailability = withContext(Dispatchers.IO) {
        try {
            var availability = ArCoreApk.getInstance().checkAvailability(context)
            var attempts = 0
            // When transient, wait briefly up to 1 second for ARCore availability to settle
            while (availability.isTransient && attempts < 5) {
                kotlinx.coroutines.delay(200)
                availability = ArCoreApk.getInstance().checkAvailability(context)
                attempts++
            }
            mapAvailability(availability)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ARCore availability", e)
            ArAvailability.CHECK_ERROR
        }
    }

    private fun mapAvailability(availability: ArCoreApk.Availability): ArAvailability {
        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArAvailability.SUPPORTED_INSTALLED
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArAvailability.SUPPORTED_NOT_INSTALLED
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArAvailability.SUPPORTED_APK_TOO_OLD
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArAvailability.UNSUPPORTED_DEVICE_NOT_CAPABLE
            ArCoreApk.Availability.UNKNOWN_CHECKING -> ArAvailability.CHECKING
            ArCoreApk.Availability.UNKNOWN_ERROR,
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> ArAvailability.CHECK_ERROR
        }
    }

    /**
     * Requests installation of Google Play Services for AR if supported but not yet installed.
     * Returns true if already installed, or false if installation prompt is shown or declined.
     */
    fun requestInstall(activity: Activity, userRequested: Boolean = true): Boolean {
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, userRequested)) {
                ArCoreApk.InstallStatus.INSTALLED -> true
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> false
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.w(TAG, "User declined ARCore installation", e)
            false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible with ARCore", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error requesting ARCore installation", e)
            false
        }
    }

    /**
     * Evaluates whether depth mode is supported by an active ARCore session.
     */
    fun isDepthModeSupported(session: Session?): Boolean {
        if (session == null) return false
        return try {
            session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query depth mode support", e)
            false
        }
    }
}
