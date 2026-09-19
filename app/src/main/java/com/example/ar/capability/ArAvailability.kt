package com.example.ar.capability

/**
 * Represents the device's Google ARCore capability and installation status.
 */
enum class ArAvailability {
    /** Checking device capability asynchronously. */
    CHECKING,

    /** ARCore is supported and installed on this device. */
    SUPPORTED_INSTALLED,

    /** ARCore is supported, but Google Play Services for AR is not installed. */
    SUPPORTED_NOT_INSTALLED,

    /** ARCore is supported, but the installed APK version is too old. */
    SUPPORTED_APK_TOO_OLD,

    /** This device does not have the hardware or software capabilities required for ARCore. */
    UNSUPPORTED_DEVICE_NOT_CAPABLE,

    /** Transient or network error checking ARCore availability. */
    CHECK_ERROR
}
