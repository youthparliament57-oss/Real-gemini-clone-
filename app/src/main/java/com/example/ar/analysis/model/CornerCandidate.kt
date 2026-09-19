package com.example.ar.analysis.model

/**
 * Domain representation of an estimated room corner where two detected walls meet near the floor.
 * Contains no ARCore references.
 */
data class CornerCandidate(
    val id: String,
    val position: SpatialVector3,
    val wallIdA: String,
    val wallIdB: String,
    val angleDegrees: Float,
    val confidence: Float
)
