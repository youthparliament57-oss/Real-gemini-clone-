package com.example.ar.analysis.model

/**
 * Runtime intermediate representation of the scanned room environment.
 * Decoupled from persistent room models and native ARCore objects.
 */
data class RoomScanResult(
    val state: ScanningState,
    val floor: FloorCandidate?,
    val walls: List<WallCandidate>,
    val candidateCorners: List<CornerCandidate>,
    val confidence: Float,
    val quality: ScanQuality,
    val isReady: Boolean,
    val guidanceText: String,
    val isDepthAvailable: Boolean,
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    companion object {
        fun initial(isDepthAvailable: Boolean = false): RoomScanResult = RoomScanResult(
            state = ScanningState.SCANNING,
            floor = null,
            walls = emptyList(),
            candidateCorners = emptyList(),
            confidence = 0.0f,
            quality = ScanQuality.POOR,
            isReady = false,
            guidanceText = "Move the phone slowly and scan the floor.",
            isDepthAvailable = isDepthAvailable
        )
    }
}
