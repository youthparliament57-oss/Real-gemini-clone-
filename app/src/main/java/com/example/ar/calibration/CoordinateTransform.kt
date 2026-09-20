package com.example.ar.calibration

import com.example.ar.analysis.model.SpatialVector3

/**
 * Mathematically handles rigid body coordinate transformations between:
 * - ARCore World Space
 * - Room Space
 * - Wall Space
 */
class CoordinateTransform(private val result: CalibrationResult) {

    /**
     * Converts a 3D point from ARCore World Space to Room Space.
     */
    fun worldToRoom(point: SpatialVector3): SpatialVector3 {
        val d = point - result.roomOrigin
        return SpatialVector3(
            d.dot(result.xAxis),
            d.dot(result.yAxis),
            d.dot(result.zAxis)
        )
    }

    /**
     * Converts a 3D point from Room Space to ARCore World Space.
     */
    fun roomToWorld(point: SpatialVector3): SpatialVector3 {
        return result.roomOrigin +
                (result.xAxis * point.x) +
                (result.yAxis * point.y) +
                (result.zAxis * point.z)
    }

    /**
     * Converts a 3D point from Room Space to Wall Space.
     */
    fun roomToWall(point: SpatialVector3, wall: WallDefinition): SpatialVector3 {
        val d = point - wall.originInRoom
        return SpatialVector3(
            d.dot(wall.xAxis),
            d.dot(wall.yAxis),
            d.dot(wall.zAxis)
        )
    }

    /**
     * Converts a 3D point from Wall Space to Room Space.
     */
    fun wallToRoom(point: SpatialVector3, wall: WallDefinition): SpatialVector3 {
        return wall.originInRoom +
                (wall.xAxis * point.x) +
                (wall.yAxis * point.y) +
                (wall.zAxis * point.z)
    }

    /**
     * Convenience: Converts directly from World Space to Wall Space.
     */
    fun worldToWall(point: SpatialVector3, wall: WallDefinition): SpatialVector3 {
        return roomToWall(worldToRoom(point), wall)
    }

    /**
     * Convenience: Converts directly from Wall Space to World Space.
     */
    fun wallToWorld(point: SpatialVector3, wall: WallDefinition): SpatialVector3 {
        return roomToWorld(wallToRoom(point, wall))
    }
}
