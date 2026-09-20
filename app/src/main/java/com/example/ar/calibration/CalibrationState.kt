package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3

enum class CalibrationMethod {
    AUTOMATIC,
    ASSISTED,
    MANUAL
}

enum class CalibrationStatus {
    SCAN,
    AUTO_CALIBRATION,
    AUTO_CALIBRATED,
    ASSISTED_CALIBRATION,
    MANUAL_CALIBRATION,
    CALIBRATED,
    CALIBRATION_FAILED,
    CALIBRATION_RETRY
}

sealed class CalibrationState {
    abstract val status: CalibrationStatus

    object Scan : CalibrationState() {
        override val status = CalibrationStatus.SCAN
    }

    object AutoCalibration : CalibrationState() {
        override val status = CalibrationStatus.AUTO_CALIBRATION
    }

    object AutoCalibrated : CalibrationState() {
        override val status = CalibrationStatus.AUTO_CALIBRATED
    }

    data class AssistedCalibration(
        val candidateCorners: List<SpatialVector3>,
        val confirmedCorners: List<SpatialVector3>
    ) : CalibrationState() {
        override val status = CalibrationStatus.ASSISTED_CALIBRATION
    }

    data class ManualCalibration(
        val cornerIndex: Int, // 0 to 3 for Corner 1 to Corner 4
        val collectedCorners: List<SpatialVector3>
    ) : CalibrationState() {
        override val status = CalibrationStatus.MANUAL_CALIBRATION
    }

    data class Calibrated(val result: CalibrationResult) : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATED
    }

    data class CalibrationFailed(val reason: String) : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATION_FAILED
    }

    object CalibrationRetry : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATION_RETRY
    }
}
