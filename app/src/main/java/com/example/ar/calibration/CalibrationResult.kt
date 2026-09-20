package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3

/**
 * Domain-friendly representation of a completed calibration.
 * Independent of temporary ARCore objects (Session, Plane, Pose, Anchor, etc.).
 * All spatial coordinates remain in meters.
 */
data class CalibrationResult(
    val roomOrigin: SpatialVector3,               // Origin (Corner 1) in ARCore World Space
    val floorNormal: SpatialVector3,              // Unit normal of the floor plane in World Space
    val xAxis: SpatialVector3,                    // Unit vector for Room X in World Space
    val yAxis: SpatialVector3,                    // Unit vector for Room Y in World Space
    val zAxis: SpatialVector3,                    // Unit vector for Room Z in World Space
    val calibratedCornersWorld: List<SpatialVector3>,  // Physical corners in World Space
    val calibratedCornersRoom: List<SpatialVector3>,   // Physical corners in Room Space (Z-up/Y-up aligned)
    val wallDefinitions: List<WallDefinition>,    // Reconstructed local wall frames
    val roomWidth: Float,                         // Maximum width of floor footprint (m)
    val roomDepth: Float,                         // Maximum depth of floor footprint (m)
    val ceilingHeight: Float,                     // User-defined ceiling height (m)
    val calibrationMethod: CalibrationMethod,     // Method used to obtain the calibration
    val confidence: Float,                        // Quality confidence score [0.0 - 1.0]
    val schemaVersion: String = "1.0.0"
)
