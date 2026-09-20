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
    CEILING_HEIGHT_ENTRY,
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

    data class AutoCalibrated(val result: CalibrationResult) : CalibrationState() {
        override val status = CalibrationStatus.AUTO_CALIBRATED
    }

    data class AssistedCalibration(
        val candidateCorners: List<SpatialVector3>,
        val confirmedCorners: List<SpatialVector3>
    ) : CalibrationState() {
        override val status = CalibrationStatus.ASSISTED_CALIBRATION
    }

    data class ManualCalibration(
        val stage: ManualCornerStage = ManualCornerStage.CORNER_1,
        val collectedCorners: List<SpatialVector3> = emptyList(),
        val cornerIndex: Int = collectedCorners.size
    ) : CalibrationState() {
        override val status = CalibrationStatus.MANUAL_CALIBRATION
    }

    data class CeilingHeightEntry(
        val provisionalResult: CalibrationResult
    ) : CalibrationState() {
        override val status = CalibrationStatus.CEILING_HEIGHT_ENTRY
    }

    data class Calibrated(val result: CalibrationResult) : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATED
    }

    data class CalibrationFailed(
        val reason: String,
        val canRetry: Boolean = true
    ) : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATION_FAILED
    }

    object CalibrationRetry : CalibrationState() {
        override val status = CalibrationStatus.CALIBRATION_RETRY
    }
}
