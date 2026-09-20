package com.example.ar.calibration

import com.example.ar.analysis.model.RoomScanResult
import com.example.ar.analysis.model.SpatialPoint2D
import com.example.ar.analysis.model.SpatialVector3
import com.example.ar.calibration.model.CalibrationConfidence
import com.example.ar.calibration.model.WallModel
import com.example.ar.calibration.model.WallSource
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Manager component responsible for room calibration logic and transformations.
 * Performs mathematical validations, coordinates states, validates corner geometry,
 * builds orthonormal Room and Wall coordinate frames, and calculates confidence ratings.
 */
class CalibrationManager {

    companion object {
        const val DEFAULT_CEILING_HEIGHT_METERS = 3.048f // ~10 feet convenience default
        const val MIN_CEILING_HEIGHT_METERS = 1.5f
        const val MAX_CEILING_HEIGHT_METERS = 12.0f
        const val MIN_CORNER_DISTANCE_METERS = 0.5f
        const val MIN_EDGE_LENGTH_METERS = 0.5f
        const val MAX_FLOOR_DEVIATION_METERS = 0.15f
        const val MIN_ROOM_AREA_SQ_METERS = 1.0f
        const val MAX_ROOM_AREA_SQ_METERS = 2500.0f
        const val MIN_ROOM_DIMENSION_METERS = 0.8f
        const val MAX_ROOM_DIMENSION_METERS = 50.0f
    }

    /**
     * Sorts 3D corners in a clockwise winding order on the horizontal floor plane around their centroid.
     */
    fun sortCornersClockwise(corners: List<SpatialVector3>): List<SpatialVector3> {
        if (corners.size < 3) return corners
        val centerX = corners.map { it.x }.average().toFloat()
        val centerZ = corners.map { it.z }.average().toFloat()
        return corners.sortedBy { c ->
            atan2(c.z - centerZ, c.x - centerX)
        }
    }

    /**
     * Checks whether two line segments in 2D (x, z) intersect.
     */
    fun doSegmentsIntersect(
        p1: SpatialPoint2D,
        p2: SpatialPoint2D,
        p3: SpatialPoint2D,
        p4: SpatialPoint2D
    ): Boolean {
        fun crossProduct(a: SpatialPoint2D, b: SpatialPoint2D, c: SpatialPoint2D): Float {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        }

        val cp1 = crossProduct(p1, p2, p3)
        val cp2 = crossProduct(p1, p2, p4)
        val cp3 = crossProduct(p3, p4, p1)
        val cp4 = crossProduct(p3, p4, p2)

        val straddles1 = (cp1 > 1e-4f && cp2 < -1e-4f) || (cp1 < -1e-4f && cp2 > 1e-4f)
        val straddles2 = (cp3 > 1e-4f && cp4 < -1e-4f) || (cp3 < -1e-4f && cp4 > 1e-4f)

        return straddles1 && straddles2
    }

    /**
     * Computes the 2D polygon area of quadrilateral corners on the horizontal floor plane using the Shoelace formula.
     */
    fun computePolygonArea2D(corners: List<SpatialVector3>): Float {
        if (corners.size < 3) return 0f
        var sum = 0.0
        val n = corners.size
        for (i in 0 until n) {
            val curr = corners[i]
            val next = corners[(i + 1) % n]
            sum += (curr.x.toDouble() * next.z.toDouble()) - (next.x.toDouble() * curr.z.toDouble())
        }
        return abs(sum * 0.5).toFloat()
    }

    /**
     * Validates whether the 4 collected corners form a physically plausible room geometry.
     */
    fun validateCalibration(
        corners: List<SpatialVector3>,
        ceilingHeight: Float
    ): Pair<Boolean, String?> {
        if (corners.size != 4) {
            return false to "Calibration requires exactly 4 distinct floor corners."
        }

        // 1. Check distinct corners & pairwise distances
        for (i in 0 until 3) {
            for (j in i + 1 until 4) {
                val dist = corners[i].distanceTo(corners[j])
                if (dist < MIN_CORNER_DISTANCE_METERS) {
                    return false to "Corners $i and $j are too close (${String.format("%.2f", dist)}m < ${MIN_CORNER_DISTANCE_METERS}m). Please select distinct corners."
                }
            }
        }

        // 2. Edge length validation
        for (i in 0 until 4) {
            val next = (i + 1) % 4
            val edgeLen = corners[i].distanceTo(corners[next])
            if (edgeLen < MIN_EDGE_LENGTH_METERS) {
                return false to "Edge between corner ${i + 1} and ${next + 1} is too short (${String.format("%.2f", edgeLen)}m < ${MIN_EDGE_LENGTH_METERS}m)."
            }
        }

        // 3. Coplanar check (all corners must sit on roughly the same horizontal floor elevation)
        val averageY = corners.map { it.y }.average().toFloat()
        for (i in corners.indices) {
            val dev = abs(corners[i].y - averageY)
            if (dev > MAX_FLOOR_DEVIATION_METERS) {
                return false to "Selected corner ${i + 1} deviates by ${String.format("%.2f", dev)}m from the floor plane (max ${MAX_FLOOR_DEVIATION_METERS}m)."
            }
        }

        // 4. Polygon self-intersection check (e.g. hourglass / crossed bowtie polygon)
        val p2d = corners.map { SpatialPoint2D(it.x, it.z) }
        val edge01_intersects_edge23 = doSegmentsIntersect(p2d[0], p2d[1], p2d[2], p2d[3])
        val edge12_intersects_edge30 = doSegmentsIntersect(p2d[1], p2d[2], p2d[3], p2d[0])
        if (edge01_intersects_edge23 || edge12_intersects_edge30) {
            return false to "Floor polygon is self-intersecting (crossed/hourglass shape). Corners must be captured in circular perimeter order."
        }

        // 5. Area and non-degeneracy check
        val area = computePolygonArea2D(corners)
        if (area < MIN_ROOM_AREA_SQ_METERS) {
            return false to "Room floor area (${String.format("%.2f", area)}m²) is too small for a plausible room (min ${MIN_ROOM_AREA_SQ_METERS}m²)."
        }
        if (area > MAX_ROOM_AREA_SQ_METERS) {
            return false to "Room floor area (${String.format("%.1f", area)}m²) exceeds plausible bounds (max ${MAX_ROOM_AREA_SQ_METERS}m²)."
        }

        // 6. Check interior angles to avoid degenerate collinear corners
        for (i in 0 until 4) {
            val prev = corners[(i + 3) % 4]
            val curr = corners[i]
            val next = corners[(i + 1) % 4]
            val v1 = (prev - curr).normalized()
            val v2 = (next - curr).normalized()
            val dot = v1.dot(v2).coerceIn(-1.0f, 1.0f)
            val angleDeg = Math.toDegrees(acos(dot.toDouble())).toFloat()
            if (angleDeg < 15.0f || angleDeg > 165.0f) {
                return false to "Interior angle at corner ${i + 1} (${String.format("%.1f", angleDeg)}°) is near-collinear or degenerate."
            }
        }

        // 7. Orthonormal axis generation check (Corner 1 -> Corner 2 must not be vertically aligned)
        val transform = CoordinateTransform.createOrthonormal(corners[0], corners[1])
        if (transform == null || !transform.isOrthonormal()) {
            return false to "Corner 1 and Corner 2 create a degenerate coordinate axis."
        }

        // 8. Physical bounding dimensions in Room Space
        val roomCorners = corners.map { transform.worldToRoom(it) }
        val xs = roomCorners.map { it.x }
        val zs = roomCorners.map { it.z }
        val width = xs.maxOrNull()!! - xs.minOrNull()!!
        val depth = zs.maxOrNull()!! - zs.minOrNull()!!

        if (width < MIN_ROOM_DIMENSION_METERS || width > MAX_ROOM_DIMENSION_METERS ||
            depth < MIN_ROOM_DIMENSION_METERS || depth > MAX_ROOM_DIMENSION_METERS
        ) {
            return false to "Plausible room size exceeded. Width: ${String.format("%.2f", width)}m, Depth: ${String.format("%.2f", depth)}m."
        }

        // 9. Ceiling height validation
        if (ceilingHeight < MIN_CEILING_HEIGHT_METERS || ceilingHeight > MAX_CEILING_HEIGHT_METERS) {
            return false to "Ceiling height (${String.format("%.2f", ceilingHeight)}m) must be between ${MIN_CEILING_HEIGHT_METERS}m and ${MAX_CEILING_HEIGHT_METERS}m."
        }

        return true to null
    }

    /**
     * Calculates the calibration confidence score [0.0 - 1.0] and categorizes it into [CalibrationConfidence].
     */
    fun calculateConfidence(
        corners: List<SpatialVector3>,
        method: CalibrationMethod,
        scanResult: RoomScanResult? = null
    ): Pair<Float, CalibrationConfidence> {
        if (corners.size != 4) return 0f to CalibrationConfidence.INVALID

        val (isValid, _) = validateCalibration(corners, DEFAULT_CEILING_HEIGHT_METERS)
        if (!isValid) return 0f to CalibrationConfidence.INVALID

        // Coplanarity variance factor
        val averageY = corners.map { it.y }.average().toFloat()
        val maxDev = corners.maxOf { abs(it.y - averageY) }
        val coplanarScore = (1.0f - (maxDev / MAX_FLOOR_DEVIATION_METERS)).coerceIn(0f, 1f)

        // Angle regularity factor (how close angles are to 90 degrees)
        var angleScoreSum = 0f
        for (i in 0 until 4) {
            val prev = corners[(i + 3) % 4]
            val curr = corners[i]
            val next = corners[(i + 1) % 4]
            val v1 = (prev - curr).normalized()
            val v2 = (next - curr).normalized()
            val dot = v1.dot(v2).coerceIn(-1.0f, 1.0f)
            val angleDeg = Math.toDegrees(acos(dot.toDouble())).toFloat()
            val devFrom90 = abs(angleDeg - 90.0f)
            val score = (1.0f - (devFrom90 / 60.0f)).coerceIn(0.2f, 1.0f)
            angleScoreSum += score
        }
        val angleScore = angleScoreSum / 4.0f

        // Area plausibility factor
        val area = computePolygonArea2D(corners)
        val areaScore = if (area in 3.0f..300.0f) 1.0f else 0.8f

        // Scan support factor
        val scanScore = scanResult?.confidence ?: 0.9f

        val baseScore = when (method) {
            CalibrationMethod.AUTOMATIC -> 0.25f * coplanarScore + 0.35f * angleScore + 0.40f * scanScore
            CalibrationMethod.ASSISTED -> 0.30f * coplanarScore + 0.30f * angleScore + 0.40f * scanScore
            CalibrationMethod.MANUAL -> 0.40f * coplanarScore + 0.40f * angleScore + 0.20f * areaScore
        }.coerceIn(0f, 1f)

        val rating = when {
            baseScore >= 0.80f -> CalibrationConfidence.HIGH
            baseScore >= 0.60f -> CalibrationConfidence.MEDIUM
            baseScore >= 0.35f -> CalibrationConfidence.LOW
            else -> CalibrationConfidence.INVALID
        }

        return baseScore to rating
    }

    /**
     * Builds a full [CalibrationResult] from 4 confirmed/collected corners, preserving the measured quadrilateral.
     */
    fun buildCalibrationResult(
        rawCorners: List<SpatialVector3>,
        ceilingHeight: Float,
        method: CalibrationMethod,
        confidence: Float? = null
    ): CalibrationResult? {
        if (rawCorners.size != 4) return null

        val (isValid, _) = validateCalibration(rawCorners, ceilingHeight)
        if (!isValid) return null

        val roomOrigin = rawCorners[0]
        val floorNormal = SpatialVector3(0f, 1f, 0f) // gravity-aligned vertical

        // Construct orthonormal Room Space transform
        val transform = CoordinateTransform.createOrthonormal(rawCorners[0], rawCorners[1], floorNormal) ?: return null

        // Map corners to Room coordinates (preserving irregular quadrilateral)
        val calibratedCornersRoom = rawCorners.map { transform.worldToRoom(it) }

        val xs = calibratedCornersRoom.map { it.x }
        val zs = calibratedCornersRoom.map { it.z }
        val roomWidth = xs.maxOrNull()!! - xs.minOrNull()!!
        val roomDepth = zs.maxOrNull()!! - zs.minOrNull()!!
        val area = computePolygonArea2D(rawCorners)

        val (calculatedConfidence, confidenceRating) = calculateConfidence(rawCorners, method)
        val finalConfidence = confidence ?: calculatedConfidence

        // Synthesize rich application-level Wall models
        val wallModels = generateWallModels(rawCorners, ceilingHeight, confidenceRating)
        val wallDefinitions = generateWallDefinitions(calibratedCornersRoom, ceilingHeight)

        return CalibrationResult(
            roomOrigin = roomOrigin,
            floorNormal = floorNormal,
            xAxis = transform.xAxis,
            yAxis = transform.yAxis,
            zAxis = transform.zAxis,
            calibratedCornersWorld = rawCorners,
            calibratedCornersRoom = calibratedCornersRoom,
            wallDefinitions = wallDefinitions,
            walls = wallModels,
            roomWidth = roomWidth,
            roomDepth = roomDepth,
            floorAreaSqMeters = area,
            ceilingHeight = ceilingHeight,
            calibrationMethod = method,
            confidence = finalConfidence,
            confidenceRating = confidenceRating,
            coordinateTransform = transform
        )
    }

    /**
     * Attempts automatic calibration using RoomAnalyzer scan results.
     * Rejects low-confidence automatic results and requires high-confidence geometry.
     */
    fun attemptAutoCalibration(
        scanResult: RoomScanResult,
        ceilingHeight: Float = DEFAULT_CEILING_HEIGHT_METERS
    ): CalibrationResult? {
        if (!scanResult.isReady || scanResult.confidence < 0.70f) {
            return null
        }
        val candidatePositions = scanResult.candidateCorners.map { it.position }
        if (candidatePositions.size < 4) {
            return null
        }

        // Take top 4 highest confidence corners and sort clockwise
        val top4Corners = scanResult.candidateCorners
            .sortedByDescending { it.confidence }
            .take(4)
            .map { it.position }

        val sortedCorners = sortCornersClockwise(top4Corners)
        val (isValid, _) = validateCalibration(sortedCorners, ceilingHeight)
        if (!isValid) {
            return null
        }

        return buildCalibrationResult(
            rawCorners = sortedCorners,
            ceilingHeight = ceilingHeight,
            method = CalibrationMethod.AUTOMATIC,
            confidence = scanResult.confidence
        )
    }

    /**
     * Generates rich application-level [WallModel]s from calibrated corners in World Space.
     */
    fun generateWallModels(
        cornersInWorld: List<SpatialVector3>,
        ceilingHeight: Float,
        confidence: CalibrationConfidence
    ): List<WallModel> {
        val walls = mutableListOf<WallModel>()
        val n = cornersInWorld.size

        // Room centroid in World Space
        val centroid = SpatialVector3(
            cornersInWorld.map { it.x }.average().toFloat(),
            cornersInWorld.map { it.y }.average().toFloat(),
            cornersInWorld.map { it.z }.average().toFloat()
        )

        for (i in 0 until n) {
            val startIdx = i
            val endIdx = (i + 1) % n
            val start = cornersInWorld[startIdx]
            val end = cornersInWorld[endIdx]

            val wallId = "wall_${i + 1}"
            val origin = start
            val length = start.distanceTo(end)

            // Horizontal tangent in World Space
            val tangentRaw = end - start
            val tangent = SpatialVector3(tangentRaw.x, 0f, tangentRaw.z).normalized()
            val up = SpatialVector3(0f, 1f, 0f)

            // Inward normal (pointing towards room centroid)
            var normal = tangent.cross(up).normalized()
            val toCentroid = centroid - start
            if (toCentroid.dot(normal) < 0f) {
                normal = normal * -1f
            }

            walls.add(
                WallModel(
                    wallId = wallId,
                    startCornerIndex = startIdx,
                    endCornerIndex = endIdx,
                    startCorner = start,
                    endCorner = end,
                    wallOrigin = origin,
                    tangent = tangent,
                    upDirection = up,
                    normal = normal,
                    lengthMeters = length,
                    heightMeters = ceilingHeight,
                    confidence = confidence,
                    detectionSource = WallSource.FLOOR_CORNER_EXTRUSION
                )
            )
        }
        return walls
    }

    /**
     * Generates legacy [WallDefinition]s in Room Space for backward compatibility.
     */
    private fun generateWallDefinitions(
        cornersInRoom: List<SpatialVector3>,
        ceilingHeight: Float
    ): List<WallDefinition> {
        val walls = mutableListOf<WallDefinition>()
        val n = cornersInRoom.size

        val center = SpatialVector3(
            cornersInRoom.map { it.x }.average().toFloat(),
            cornersInRoom.map { it.y }.average().toFloat(),
            cornersInRoom.map { it.z }.average().toFloat()
        )

        for (i in 0 until n) {
            val startIdx = i
            val endIdx = (i + 1) % n
            val start = cornersInRoom[startIdx]
            val end = cornersInRoom[endIdx]

            val wallId = "wall_${i + 1}"
            val origin = start
            val length = start.distanceTo(end)

            val tangentRaw = end - start
            val tangent = SpatialVector3(tangentRaw.x, 0f, tangentRaw.z).normalized()
            val vertical = SpatialVector3(0f, 1f, 0f)

            var normal = tangent.cross(vertical).normalized()
            val toCenter = center - start
            if (toCenter.dot(normal) < 0f) {
                normal = normal * -1f
            }

            walls.add(
                WallDefinition(
                    id = wallId,
                    originInRoom = origin,
                    xAxis = tangent,
                    yAxis = vertical,
                    zAxis = normal,
                    length = length,
                    height = ceilingHeight,
                    startCornerIndex = startIdx,
                    endCornerIndex = endIdx
                )
            )
        }
        return walls
    }
}
