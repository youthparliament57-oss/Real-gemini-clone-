package com.example.ar.calibration.model

/**
 * Quality rating of room calibration geometry and tracking stability.
 */
enum class CalibrationConfidence(val label: String) {
    HIGH("High Quality"),
    MEDIUM("Medium Quality"),
    LOW("Low Confidence"),
    INVALID("Invalid Geometry");

    fun isAcceptable(): Boolean = this == HIGH || this == MEDIUM
}
