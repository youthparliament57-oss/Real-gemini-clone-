package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.model.CalibrationConfidence
import com.example.ar.calibration.model.RoomDimensions
import com.example.ar.calibration.model.RoomModel
import com.example.ar.calibration.model.WallModel

/**
 * Domain representation of a completed room calibration.
 * Independent of temporary ARCore objects (Session, Plane, Pose, Anchor, etc.).
 * All spatial coordinates remain in meters.
 */
data class CalibrationResult(
    val roomOrigin: SpatialVector3,                   // Origin (Corner 1) in ARCore World Space
    val floorNormal: SpatialVector3,                  // Unit normal of the floor plane in World Space
    val xAxis: SpatialVector3,                        // Unit vector for Room X in World Space
    val yAxis: SpatialVector3,                        // Unit vector for Room Y in World Space
    val zAxis: SpatialVector3,                        // Unit vector for Room Z in World Space
    val calibratedCornersWorld: List<SpatialVector3>, // Physical corners in World Space
    val calibratedCornersRoom: List<SpatialVector3>,  // Physical corners in Room Space
    val wallDefinitions: List<WallDefinition>,        // Reconstructed local wall frames
    val walls: List<WallModel> = emptyList(),         // Rich application-level wall models
    val roomWidth: Float,                             // Maximum width of floor footprint (m)
    val roomDepth: Float,                             // Maximum depth of floor footprint (m)
    val floorAreaSqMeters: Float = roomWidth * roomDepth, // Estimated floor area (m²)
    val ceilingHeight: Float,                         // User-defined ceiling height (m)
    val calibrationMethod: CalibrationMethod,         // Method used to obtain the calibration
    val confidence: Float,                            // Numerical quality confidence score [0.0 - 1.0]
    val confidenceRating: CalibrationConfidence = CalibrationConfidence.HIGH, // Categorical confidence
    val coordinateTransform: CoordinateTransform = CoordinateTransform(roomOrigin, xAxis, yAxis, zAxis),
    val schemaVersion: String = "1.0.0"
) {
    fun toRoomModel(): RoomModel {
        val dimensions = RoomDimensions(
            widthMeters = roomWidth,
            lengthMeters = roomDepth,
            areaSqMeters = floorAreaSqMeters,
            perimeterMeters = run {
                var p = 0f
                for (i in calibratedCornersWorld.indices) {
                    val next = (i + 1) % calibratedCornersWorld.size
                    p += calibratedCornersWorld[i].distanceTo(calibratedCornersWorld[next])
                }
                p
            }
        )
        return RoomModel(
            origin = roomOrigin,
            coordinateFrame = coordinateTransform,
            floorPolygon = calibratedCornersWorld,
            floorPolygonRoom = calibratedCornersRoom,
            calibratedCorners = calibratedCornersWorld,
            roomDimensions = dimensions,
            calibrationMethod = calibrationMethod,
            calibrationConfidence = confidenceRating,
            schemaVersion = 1,
            ceilingHeightMeters = ceilingHeight,
            walls = walls
        )
    }
}
