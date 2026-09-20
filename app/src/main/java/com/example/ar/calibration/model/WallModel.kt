package com.example.ar.calibration.model

import com.example.ar.analysis.model.SpatialVector3

/**
 * Application-level domain model representing a physical/synthesized room wall.
 *
 * Wall Coordinate Frame:
 * - U (Horizontal tangent): Vector along the wall from start corner towards end corner [0 <= u <= lengthMeters]
 * - V (Vertical up): Gravity-aligned upward vector [0 <= v <= heightMeters]
 * - N (Inward normal): Unit vector perpendicular to the wall pointing directly into the room interior
 *
 * This frame enables future wall-mounted objects/panels to position themselves via (u, v, n) offsets.
 */
data class WallModel(
    val wallId: String,
    val startCornerIndex: Int,
    val endCornerIndex: Int,
    val startCorner: SpatialVector3,
    val endCorner: SpatialVector3,
    val wallOrigin: SpatialVector3,
    val tangent: SpatialVector3,
    val upDirection: SpatialVector3,
    val normal: SpatialVector3,
    val lengthMeters: Float,
    val heightMeters: Float,
    val confidence: CalibrationConfidence,
    val detectionSource: WallSource
) {
    /**
     * Converts a local wall coordinate (u: horizontal offset, v: vertical height, n: normal offset)
     * into ARCore World Space coordinates.
     */
    fun wallLocalToWorld(u: Float, v: Float, n: Float = 0f): SpatialVector3 {
        return wallOrigin + (tangent * u) + (upDirection * v) + (normal * n)
    }

    /**
     * Converts an ARCore World Space point into the local wall coordinate frame:
     * - x: horizontal offset (u) along wall tangent
     * - y: vertical height (v) from floor baseline
     * - z: normal offset (n) into the room interior
     */
    fun worldToWallLocal(worldPoint: SpatialVector3): SpatialVector3 {
        val delta = worldPoint - wallOrigin
        return SpatialVector3(
            delta.dot(tangent),
            delta.dot(upDirection),
            delta.dot(normal)
        )
    }
}
