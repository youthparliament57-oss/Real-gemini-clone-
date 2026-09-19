package com.example.gemini.ui.stark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StarkWidgetView(
    type: StarkWidgetType,
    isPinned: Boolean = false,
    isMinimized: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onMinimizeToggle: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    onRelocate: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (type) {
        StarkWidgetType.ARC_REACTOR -> {
            StarkArcReactorWidget(
                isPinned = isPinned,
                isMinimized = isMinimized,
                onPinToggle = onPinToggle,
                onMinimizeToggle = onMinimizeToggle,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onRelocate = onRelocate,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.VISION_SCANNER -> {
            StarkVisionScannerWidget(
                isPinned = isPinned,
                isMinimized = isMinimized,
                onPinToggle = onPinToggle,
                onMinimizeToggle = onMinimizeToggle,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onRelocate = onRelocate,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.MISSION_MATRIX -> {
            StarkMissionMatrixWidget(
                isPinned = isPinned,
                isMinimized = isMinimized,
                onPinToggle = onPinToggle,
                onMinimizeToggle = onMinimizeToggle,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onRelocate = onRelocate,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.QUANTUM_ENVIRONMENT -> {
            StarkQuantumClockWidget(
                isPinned = isPinned,
                isMinimized = isMinimized,
                onPinToggle = onPinToggle,
                onMinimizeToggle = onMinimizeToggle,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onRelocate = onRelocate,
                onClose = onClose,
                modifier = modifier
            )
        }
    }
}

