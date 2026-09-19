package com.example.gemini.ui.stark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StarkWidgetView(
    type: StarkWidgetType,
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (type) {
        StarkWidgetType.ARC_REACTOR -> {
            StarkArcReactorWidget(
                isPinned = isPinned,
                onPinToggle = onPinToggle,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.VISION_SCANNER -> {
            StarkVisionScannerWidget(
                isPinned = isPinned,
                onPinToggle = onPinToggle,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.MISSION_MATRIX -> {
            StarkMissionMatrixWidget(
                isPinned = isPinned,
                onPinToggle = onPinToggle,
                onClose = onClose,
                modifier = modifier
            )
        }
        StarkWidgetType.QUANTUM_ENVIRONMENT -> {
            StarkQuantumClockWidget(
                isPinned = isPinned,
                onPinToggle = onPinToggle,
                onClose = onClose,
                modifier = modifier
            )
        }
    }
}
