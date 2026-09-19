package com.example.ar.analysis.model

/**
 * Domain representation of a detected vertical wall candidate.
 * Contains no ARCore references.
 */
data class WallCandidate(
    val id: String,
    val center: SpatialVector3,
    val normal: SpatialVector3,
    val width: Float,
    val height: Float,
    val boundaryPolygon: List<SpatialPoint2D>,
    val confidence: Float,
    val stabilityCount: Int,
    val distanceToFloor: Float,
    val azimuthDegrees: Float
)
