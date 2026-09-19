package com.example.ar.analysis.model

/**
 * Domain representation of a detected horizontal floor candidate.
 * Contains no ARCore references.
 */
data class FloorCandidate(
    val id: String,
    val center: SpatialVector3,
    val elevationY: Float,
    val normal: SpatialVector3,
    val extentX: Float,
    val extentZ: Float,
    val area: Float,
    val boundaryPolygon: List<SpatialPoint2D>,
    val confidence: Float,
    val stabilityCount: Int,
    val isPlausibleRoomFloor: Boolean
)
