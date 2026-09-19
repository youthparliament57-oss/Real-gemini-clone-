package com.example.ar.analysis.model

/**
 * Type of detected plane surface in pure domain representations.
 */
enum class SnapshotPlaneType {
    HORIZONTAL_UPWARD_FACING,
    VERTICAL,
    OTHER
}

/**
 * Pure domain snapshot of a detected plane surface.
 * Free of all ARCore runtime objects (Pose, Plane, Session, Frame).
 */
data class PlaneSnapshot(
    val id: String,
    val type: SnapshotPlaneType,
    val isTracking: Boolean,
    val center: SpatialVector3,
    val normal: SpatialVector3,
    val extentX: Float,
    val extentZ: Float,
    val polygon: List<SpatialPoint2D> = emptyList()
) {
    val area: Float get() = extentX * extentZ
}
