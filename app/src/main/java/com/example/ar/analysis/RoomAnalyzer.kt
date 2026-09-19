package com.example.ar.analysis

import com.example.ar.analysis.model.CornerCandidate
import com.example.ar.analysis.model.FloorCandidate
import com.example.ar.analysis.model.PlaneSnapshot
import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.ScanQuality
import com.example.ar.analysis.model.ScanningState
import com.example.ar.analysis.model.SnapshotPlaneType
import com.example.ar.analysis.model.SpatialPoint2D
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.analysis.model.WallCandidate
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * Isolated Room Analyzer component.
 * Consumes plane geometry at the boundary and produces pure domain [RoomScanResult] models.
 * Does not own the ARCore Session, does not manage session lifecycle, and never leaks
 * native ARCore objects (Session, Frame, Plane, Anchor, Pose) into domain models.
 */
class RoomAnalyzer {

    companion object {
        const val MIN_FLOOR_AREA_SQ_METERS = 0.6f
        const val MIN_WALL_WIDTH_METERS = 0.4f
        const val MIN_WALL_HEIGHT_METERS = 0.4f
        const val STABILITY_OBSERVATION_THRESHOLD = 10
    }

    // Historical tracking of plane observation stability
    private data class PlaneObservation(
        val firstSeenMs: Long,
        var observationCount: Int,
        var lastArea: Float,
        var lastCenter: SpatialVector3
    )

    private val planeObservations = mutableMapOf<String, PlaneObservation>()

    /**
     * Resets observation history when starting a fresh scan.
     */
    fun reset() {
        planeObservations.clear()
    }

    /**
     * Boundary method consuming ARCore [Plane] objects and delegating to domain analysis.
     */
    fun analyze(
        planes: Collection<Plane>,
        cameraPosition: SpatialVector3?,
        isDepthAvailable: Boolean,
        isTracking: Boolean
    ): RoomScanResult {
        val snapshots = planes.map { toSnapshot(it) }
        return analyzeSnapshots(snapshots, cameraPosition, isDepthAvailable, isTracking)
    }

    /**
     * Pure domain analysis consuming [PlaneSnapshot] collections.
     * Fully decoupled from ARCore native runtime binaries, enabling fast local unit testing.
     */
    fun analyzeSnapshots(
        planes: Collection<PlaneSnapshot>,
        cameraPosition: SpatialVector3?,
        isDepthAvailable: Boolean,
        isTracking: Boolean
    ): RoomScanResult {
        if (!isTracking) {
            return RoomScanResult(
                state = ScanningState.TRACKING_LOST,
                floor = null,
                walls = emptyList(),
                candidateCorners = emptyList(),
                confidence = 0.0f,
                quality = ScanQuality.POOR,
                isReady = false,
                guidanceText = "Tracking lost. Move device slowly or improve lighting.",
                isDepthAvailable = isDepthAvailable
            )
        }

        val currentTime = System.currentTimeMillis()

        // 1. Filter actively tracking planes and update stability records
        val trackingPlanes = planes.filter { it.isTracking }
        for (plane in trackingPlanes) {
            val existing = planeObservations[plane.id]
            if (existing == null) {
                planeObservations[plane.id] = PlaneObservation(
                    firstSeenMs = currentTime,
                    observationCount = 1,
                    lastArea = plane.area,
                    lastCenter = plane.center
                )
            } else {
                existing.observationCount++
                existing.lastArea = plane.area
                existing.lastCenter = plane.center
            }
        }

        // 2. Identify candidate floors
        val floorCandidates = identifyFloorCandidates(trackingPlanes, cameraPosition, isDepthAvailable)
        val selectedFloor = floorCandidates.maxByOrNull { it.confidence }

        // 3. Identify candidate walls
        val wallCandidates = identifyWallCandidates(trackingPlanes, selectedFloor, isDepthAvailable)

        // 4. Analyze candidate corners from intersecting wall planes on the floor
        val corners = if (selectedFloor != null && wallCandidates.size >= 2) {
            analyzeCornerIntersections(selectedFloor, wallCandidates)
        } else {
            emptyList()
        }

        // 5. Evaluate overall scan confidence and quality
        val floorScore = selectedFloor?.confidence ?: 0.0f
        val wallScore = if (wallCandidates.isNotEmpty()) {
            min(1.0f, wallCandidates.map { it.confidence }.average().toFloat() * (min(4, wallCandidates.size) / 3.0f))
        } else {
            0.0f
        }
        val cornerScore = if (corners.isNotEmpty()) min(1.0f, corners.size * 0.25f) else 0.0f

        val overallConfidence = (floorScore * 0.45f) + (wallScore * 0.40f) + (cornerScore * 0.15f)

        val quality = when {
            overallConfidence >= 0.75f && wallCandidates.size >= 3 && corners.size >= 2 -> ScanQuality.EXCELLENT
            overallConfidence >= 0.55f && wallCandidates.size >= 2 -> ScanQuality.GOOD
            overallConfidence >= 0.35f && selectedFloor != null -> ScanQuality.MODERATE
            else -> ScanQuality.POOR
        }

        val isReady = selectedFloor != null &&
            selectedFloor.isPlausibleRoomFloor &&
            wallCandidates.size >= 2 &&
            overallConfidence >= 0.50f

        // 6. Determine scanning state & concise user guidance
        val (state, guidance) = when {
            selectedFloor == null || !selectedFloor.isPlausibleRoomFloor -> {
                ScanningState.SCANNING to "Move the phone slowly and point at the floor."
            }
            wallCandidates.isEmpty() -> {
                ScanningState.FLOOR_DETECTED to "Floor detected! Pan slowly across surrounding walls."
            }
            wallCandidates.size < 2 -> {
                ScanningState.WALLS_DETECTED to "Wall detected. Continue panning to capture more walls."
            }
            isReady -> {
                ScanningState.SCAN_READY to "Room scan ready! Environment geometry collected."
            }
            else -> {
                ScanningState.GEOMETRY_ANALYZING to "Analyzing room corners and wall boundaries..."
            }
        }

        return RoomScanResult(
            state = state,
            floor = selectedFloor,
            walls = wallCandidates,
            candidateCorners = corners,
            confidence = overallConfidence,
            quality = quality,
            isReady = isReady,
            guidanceText = guidance,
            isDepthAvailable = isDepthAvailable,
            lastUpdatedMs = currentTime
        )
    }

    private fun identifyFloorCandidates(
        planes: List<PlaneSnapshot>,
        cameraPosition: SpatialVector3?,
        isDepthAvailable: Boolean
    ): List<FloorCandidate> {
        val horizontalUpwardPlanes = planes.filter {
            it.type == SnapshotPlaneType.HORIZONTAL_UPWARD_FACING
        }

        if (horizontalUpwardPlanes.isEmpty()) return emptyList()

        // Find lowest horizontal plane Y beneath camera as floor baseline reference
        val elevations = horizontalUpwardPlanes.map { it.center.y }
        val minElevation = elevations.minOrNull() ?: 0.0f

        return horizontalUpwardPlanes.mapNotNull { plane ->
            val area = plane.area
            val elevationY = plane.center.y
            val observation = planeObservations[plane.id]
            val stabilityCount = observation?.observationCount ?: 1

            // Exclude tiny surfaces (e.g. stool tops)
            if (area < MIN_FLOOR_AREA_SQ_METERS) return@mapNotNull null

            // Factors for floor scoring
            val areaFactor = min(1.0f, area / 3.5f)
            val stabilityFactor = min(1.0f, stabilityCount.toFloat() / STABILITY_OBSERVATION_THRESHOLD)

            // Elevation plausibility: Floor should be close to the lowest detected horizontal surface
            val elevationDiff = abs(elevationY - minElevation)
            val elevationScore = max(0.0f, 1.0f - (elevationDiff / 0.5f))

            // Camera height plausibility: if camera height is known, floor is typically 0.8m to 2.2m below camera
            val cameraScore = if (cameraPosition != null) {
                val drop = cameraPosition.y - elevationY
                if (drop in 0.6f..2.5f) 1.0f else 0.5f
            } else {
                0.8f
            }

            val depthBonus = if (isDepthAvailable) 0.1f else 0.0f

            val confidence = min(
                1.0f,
                (areaFactor * 0.35f) +
                (stabilityFactor * 0.25f) +
                (elevationScore * 0.20f) +
                (cameraScore * 0.15f) +
                depthBonus
            )

            FloorCandidate(
                id = plane.id,
                center = plane.center,
                elevationY = elevationY,
                normal = SpatialVector3(0f, 1f, 0f),
                extentX = plane.extentX,
                extentZ = plane.extentZ,
                area = area,
                boundaryPolygon = plane.polygon,
                confidence = confidence,
                stabilityCount = stabilityCount,
                isPlausibleRoomFloor = confidence >= 0.40f && area >= MIN_FLOOR_AREA_SQ_METERS
            )
        }
    }

    private fun identifyWallCandidates(
        planes: List<PlaneSnapshot>,
        floor: FloorCandidate?,
        isDepthAvailable: Boolean
    ): List<WallCandidate> {
        val verticalPlanes = planes.filter {
            it.type == SnapshotPlaneType.VERTICAL
        }

        return verticalPlanes.mapNotNull { plane ->
            val width = plane.extentX
            val height = plane.extentZ
            val area = width * height

            if (width < MIN_WALL_WIDTH_METERS || height < MIN_WALL_HEIGHT_METERS) {
                return@mapNotNull null
            }

            val normal = plane.normal
            val azimuthDegrees = (Math.toDegrees(atan2(normal.z.toDouble(), normal.x.toDouble())).toFloat() + 360f) % 360f

            val observation = planeObservations[plane.id]
            val stabilityCount = observation?.observationCount ?: 1
            val stabilityFactor = min(1.0f, stabilityCount.toFloat() / STABILITY_OBSERVATION_THRESHOLD)
            val areaFactor = min(1.0f, area / 2.0f)

            // Distance from base of wall to detected floor
            val wallBaseY = plane.center.y - (height / 2.0f)
            val distanceToFloor = if (floor != null) abs(wallBaseY - floor.elevationY) else 0.5f
            val floorAlignmentScore = if (floor != null) {
                max(0.0f, 1.0f - (distanceToFloor / 0.8f))
            } else {
                0.5f
            }

            val depthBonus = if (isDepthAvailable) 0.1f else 0.0f

            val confidence = min(
                1.0f,
                (areaFactor * 0.35f) +
                (stabilityFactor * 0.30f) +
                (floorAlignmentScore * 0.25f) +
                depthBonus
            )

            WallCandidate(
                id = plane.id,
                center = plane.center,
                normal = normal,
                width = width,
                height = height,
                boundaryPolygon = plane.polygon,
                confidence = confidence,
                stabilityCount = stabilityCount,
                distanceToFloor = distanceToFloor,
                azimuthDegrees = azimuthDegrees
            )
        }
    }

    /**
     * Finds intersections between pairs of detected vertical walls projected onto the floor elevation.
     */
    private fun analyzeCornerIntersections(
        floor: FloorCandidate,
        walls: List<WallCandidate>
    ): List<CornerCandidate> {
        val corners = mutableListOf<CornerCandidate>()
        val floorY = floor.elevationY

        for (i in 0 until walls.size) {
            for (j in (i + 1) until walls.size) {
                val wallA = walls[i]
                val wallB = walls[j]

                // Normal vectors in the horizontal (X-Z) plane
                val nA = SpatialPoint2D(wallA.normal.x, wallA.normal.z)
                val nB = SpatialPoint2D(wallB.normal.x, wallB.normal.z)

                // Dot product of horizontal normals
                val dot = nA.x * nB.x + nA.y * nB.y
                val absDot = abs(dot)

                // Walls must not be parallel or collinear (angle between ~35° and ~145°)
                if (absDot > 0.82f) continue

                // 2D line equation: n.x * (x - c.x) + n.z * (z - c.z) = 0 => n.x * x + n.z * z = d
                val dA = nA.x * wallA.center.x + nA.y * wallA.center.z
                val dB = nB.x * wallB.center.x + nB.y * wallB.center.z

                // Solve system of 2 linear equations in X and Z
                val det = nA.x * nB.y - nA.y * nB.x
                if (abs(det) < 0.1f) continue

                val cornerX = (dA * nB.y - dB * nA.y) / det
                val cornerZ = (nA.x * dB - nB.x * dA) / det
                val cornerPos = SpatialVector3(cornerX, floorY, cornerZ)

                // Verify distance from both wall centers is reasonable
                val distA = cornerPos.distanceTo(SpatialVector3(wallA.center.x, floorY, wallA.center.z))
                val distB = cornerPos.distanceTo(SpatialVector3(wallB.center.x, floorY, wallB.center.z))

                val maxAllowedDist = max(wallA.width, wallB.width) + 2.5f
                if (distA > maxAllowedDist || distB > maxAllowedDist) continue

                // Compute angle between walls in degrees
                val angleRad = acos(absDot.coerceIn(0.0f, 1.0f))
                val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                // Corner confidence based on wall confidences, angle proximity to 90 deg, and proximity
                val angleScore = max(0.0f, 1.0f - abs(angleDeg - 90f) / 55f)
                val cornerConfidence = min(1.0f, (wallA.confidence * wallB.confidence) * 0.7f + (angleScore * 0.3f))

                corners.add(
                    CornerCandidate(
                        id = "corner_${wallA.id}_${wallB.id}",
                        position = cornerPos,
                        wallIdA = wallA.id,
                        wallIdB = wallB.id,
                        angleDegrees = angleDeg,
                        confidence = cornerConfidence
                    )
                )
            }
        }

        return corners.sortedByDescending { it.confidence }.take(8)
    }

    private fun toSnapshot(plane: Plane): PlaneSnapshot {
        val type = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> SnapshotPlaneType.HORIZONTAL_UPWARD_FACING
            Plane.Type.VERTICAL -> SnapshotPlaneType.VERTICAL
            else -> SnapshotPlaneType.OTHER
        }

        val center = toSpatialVector(plane.centerPose.translation)
        val normal = if (type == SnapshotPlaneType.VERTICAL) {
            extractVerticalPlaneNormal(plane)
        } else {
            SpatialVector3(0f, 1f, 0f)
        }

        return PlaneSnapshot(
            id = "plane_${plane.hashCode()}",
            type = type,
            isTracking = plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null,
            center = center,
            normal = normal,
            extentX = plane.extentX,
            extentZ = plane.extentZ,
            polygon = extractBoundary2D(plane.polygon)
        )
    }

    private fun extractVerticalPlaneNormal(plane: Plane): SpatialVector3 {
        return try {
            val center = plane.centerPose.translation
            val transformed = plane.centerPose.transformPoint(floatArrayOf(0f, 1f, 0f))
            val nx = transformed[0] - center[0]
            val ny = transformed[1] - center[1]
            val nz = transformed[2] - center[2]
            SpatialVector3(nx, 0f, nz).normalized()
        } catch (e: Exception) {
            SpatialVector3(0f, 0f, 1f)
        }
    }

    private fun extractBoundary2D(polygonBuffer: FloatBuffer): List<SpatialPoint2D> {
        val points = mutableListOf<SpatialPoint2D>()
        val buffer = polygonBuffer.asReadOnlyBuffer()
        buffer.rewind()
        while (buffer.remaining() >= 2) {
            val x = buffer.get()
            val z = buffer.get()
            points.add(SpatialPoint2D(x, z))
        }
        return points
    }

    private fun toSpatialVector(floatArray: FloatArray): SpatialVector3 {
        return if (floatArray.size >= 3) {
            SpatialVector3(floatArray[0], floatArray[1], floatArray[2])
        } else {
            SpatialVector3(0f, 0f, 0f)
        }
    }
}
