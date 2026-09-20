package com.example.ar.calibration.model

import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.CalibrationMethod
import com.example.ar.calibration.CoordinateTransform
import java.util.UUID

/**
 * Application-level domain model representing a fully calibrated room.
 *
 * Contains pure Kotlin data structures representing the measured spatial room geometry,
 * coordinate transform, wall frames, and ceiling bounds.
 * Completely decoupled from native ARCore runtime objects (Session, Frame, Plane, Anchor, Pose).
 */
data class RoomModel(
    val roomId: String = UUID.randomUUID().toString(),
    val origin: SpatialVector3,
    val coordinateFrame: CoordinateTransform,
    val floorPolygon: List<SpatialVector3>,
    val floorPolygonRoom: List<SpatialVector3>,
    val calibratedCorners: List<SpatialVector3>,
    val roomDimensions: RoomDimensions,
    val calibrationMethod: CalibrationMethod,
    val calibrationConfidence: CalibrationConfidence,
    val schemaVersion: Int = 1,
    val ceilingHeightMeters: Float,
    val walls: List<WallModel>,
    val metadata: Map<String, String> = emptyMap()
)
