package com.example.ar

import com.example.ar.analysis.RoomAnalyzer
import com.example.ar.analysis.model.PlaneSnapshot
import com.example.ar.analysis.model.ScanningState
import com.example.ar.analysis.model.SnapshotPlaneType
import com.example.ar.analysis.model.SpatialPoint2D
import com.example.ar.analysis.model.SpatialVector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomAnalyzerTest {

    private lateinit var analyzer: RoomAnalyzer

    @Before
    fun setUp() {
        analyzer = RoomAnalyzer()
    }

    @Test
    fun analyze_whenTrackingLost_returnsTrackingLostState() {
        val result = analyzer.analyzeSnapshots(
            planes = emptyList(),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = false,
            isTracking = false
        )

        assertEquals(ScanningState.TRACKING_LOST, result.state)
        assertFalse(result.isReady)
        assertNull(result.floor)
        assertTrue(result.walls.isEmpty())
    }

    @Test
    fun analyze_whenNoPlanesDetected_returnsScanningState() {
        val result = analyzer.analyzeSnapshots(
            planes = emptyList(),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = false,
            isTracking = true
        )

        assertEquals(ScanningState.SCANNING, result.state)
        assertFalse(result.isReady)
        assertNull(result.floor)
    }

    @Test
    fun floorDetection_distinguishesFloorFromElevatedTable() {
        // Table: elevated at Y = -0.4m, area = 0.5m x 0.5m = 0.25 m2 (tiny)
        val table = PlaneSnapshot(
            id = "table_surface",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = true,
            center = SpatialVector3(0f, -0.4f, -1.0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 0.5f,
            extentZ = 0.5f
        )

        // Room Floor: lowest surface at Y = -1.3m, large area = 3.0m x 3.0m = 9.0 m2
        val floor = PlaneSnapshot(
            id = "room_floor",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = true,
            center = SpatialVector3(0f, -1.3f, 0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 3.0f,
            extentZ = 3.0f
        )

        val cameraPos = SpatialVector3(0f, 0f, 0f)

        val result = analyzer.analyzeSnapshots(
            planes = listOf(table, floor),
            cameraPosition = cameraPos,
            isDepthAvailable = true,
            isTracking = true
        )

        assertNotNull(result.floor)
        assertEquals("room_floor", result.floor?.id)
        assertEquals(-1.3f, result.floor?.elevationY ?: 0f, 0.01f)
        assertTrue((result.floor?.confidence ?: 0f) > 0.6f)
        assertTrue(result.floor?.isPlausibleRoomFloor == true)
        assertEquals(ScanningState.FLOOR_DETECTED, result.state)
    }

    @Test
    fun wallDetection_filtersSmallObstaclesAndIdentifiesPlausibleWalls() {
        val floor = PlaneSnapshot(
            id = "room_floor",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = true,
            center = SpatialVector3(0f, -1.2f, 0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 4.0f,
            extentZ = 4.0f
        )

        // Tiny vertical obstacle (e.g. chair leg or small post: 0.1m x 0.2m)
        val smallObstacle = PlaneSnapshot(
            id = "small_obstacle",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(0.5f, -0.8f, -1.0f),
            normal = SpatialVector3(0f, 0f, 1f),
            extentX = 0.1f,
            extentZ = 0.2f
        )

        // Plausible North Wall: 3m wide, 2m high
        val northWall = PlaneSnapshot(
            id = "north_wall",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(0f, -0.2f, -2.5f),
            normal = SpatialVector3(0f, 0f, 1f),
            extentX = 3.0f,
            extentZ = 2.0f
        )

        val result = analyzer.analyzeSnapshots(
            planes = listOf(floor, smallObstacle, northWall),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = false,
            isTracking = true
        )

        assertEquals(1, result.walls.size)
        assertEquals("north_wall", result.walls.first().id)
        assertEquals(3.0f, result.walls.first().width, 0.01f)
        assertEquals(2.0f, result.walls.first().height, 0.01f)
        assertEquals(ScanningState.WALLS_DETECTED, result.state)
    }

    @Test
    fun cornerAnalysis_detectsIntersectionBetweenPerpendicularWalls() {
        val floor = PlaneSnapshot(
            id = "room_floor",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = true,
            center = SpatialVector3(0f, -1.2f, 0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 5.0f,
            extentZ = 5.0f
        )

        // North wall along Z = -2.0, normal points +Z (into the room)
        val northWall = PlaneSnapshot(
            id = "north_wall",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(0f, -0.2f, -2.0f),
            normal = SpatialVector3(0f, 0f, 1f),
            extentX = 3.0f,
            extentZ = 2.0f
        )

        // West wall along X = -1.5, normal points +X (into the room)
        val westWall = PlaneSnapshot(
            id = "west_wall",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(-1.5f, -0.2f, 0f),
            normal = SpatialVector3(1f, 0f, 0f),
            extentX = 3.0f,
            extentZ = 2.0f
        )

        // Simulate multiple observations to accumulate stability
        var scanResult = analyzer.analyzeSnapshots(
            planes = listOf(floor, northWall, westWall),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = true,
            isTracking = true
        )

        for (i in 1..10) {
            scanResult = analyzer.analyzeSnapshots(
                planes = listOf(floor, northWall, westWall),
                cameraPosition = SpatialVector3(0f, 0f, 0f),
                isDepthAvailable = true,
                isTracking = true
            )
        }

        assertEquals(2, scanResult.walls.size)
        assertEquals(1, scanResult.candidateCorners.size)

        val corner = scanResult.candidateCorners.first()
        // Intersection of X = -1.5 and Z = -2.0 at floor Y = -1.2
        assertEquals(-1.5f, corner.position.x, 0.05f)
        assertEquals(-1.2f, corner.position.y, 0.05f)
        assertEquals(-2.0f, corner.position.z, 0.05f)
        assertEquals(90.0f, corner.angleDegrees, 2.0f)
        assertTrue(corner.confidence > 0.4f)
        assertTrue(scanResult.isReady)
        assertEquals(ScanningState.SCAN_READY, scanResult.state)
    }

    @Test
    fun cornerAnalysis_ignoresParallelWalls() {
        val floor = PlaneSnapshot(
            id = "room_floor",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = true,
            center = SpatialVector3(0f, -1.2f, 0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 5.0f,
            extentZ = 5.0f
        )

        // Two parallel walls with identical normal (0, 0, 1)
        val northWall1 = PlaneSnapshot(
            id = "north_wall_1",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(-1.0f, -0.2f, -2.0f),
            normal = SpatialVector3(0f, 0f, 1f),
            extentX = 2.0f,
            extentZ = 2.0f
        )
        val northWall2 = PlaneSnapshot(
            id = "north_wall_2",
            type = SnapshotPlaneType.VERTICAL,
            isTracking = true,
            center = SpatialVector3(1.0f, -0.2f, -2.0f),
            normal = SpatialVector3(0f, 0f, 1f),
            extentX = 2.0f,
            extentZ = 2.0f
        )

        val scanResult = analyzer.analyzeSnapshots(
            planes = listOf(floor, northWall1, northWall2),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = false,
            isTracking = true
        )

        // Parallel walls do not intersect to form room corners
        assertTrue(scanResult.candidateCorners.isEmpty())
    }

    @Test
    fun untrackedPlanes_areIgnoredDuringAnalysis() {
        val untrackedFloor = PlaneSnapshot(
            id = "untracked_floor",
            type = SnapshotPlaneType.HORIZONTAL_UPWARD_FACING,
            isTracking = false,
            center = SpatialVector3(0f, -1.0f, 0f),
            normal = SpatialVector3(0f, 1f, 0f),
            extentX = 4.0f,
            extentZ = 4.0f
        )

        val result = analyzer.analyzeSnapshots(
            planes = listOf(untrackedFloor),
            cameraPosition = SpatialVector3(0f, 0f, 0f),
            isDepthAvailable = false,
            isTracking = true
        )

        assertNull(result.floor)
        assertEquals(ScanningState.SCANNING, result.state)
    }
}
