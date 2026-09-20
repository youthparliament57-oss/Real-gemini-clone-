package com.example.ar.calibration

/**
 * Sequential stages of the Manual Four-Corner Calibration workflow.
 */
enum class ManualCornerStage(
    val cornerNumber: Int,
    val stepTitle: String,
    val instruction: String
) {
    CORNER_1(
        cornerNumber = 1,
        stepTitle = "Corner 1 (Room Origin)",
        instruction = "Aim reticle at the floor corner chosen as the room's origin (0, 0, 0)."
    ),
    CORNER_2(
        cornerNumber = 2,
        stepTitle = "Corner 2 (+X Direction)",
        instruction = "Aim reticle at the adjacent floor corner along Wall 1 to define the +X axis."
    ),
    CORNER_3(
        cornerNumber = 3,
        stepTitle = "Corner 3 (Opposite Corner)",
        instruction = "Aim reticle at the diagonal/opposite floor corner along Wall 2."
    ),
    CORNER_4(
        cornerNumber = 4,
        stepTitle = "Corner 4 (Closing Corner)",
        instruction = "Aim reticle at the final floor corner to close the room perimeter."
    ),
    VALIDATING(
        cornerNumber = 4,
        stepTitle = "Validating Geometry",
        instruction = "Checking coplanarity, quadrilateral integrity, and plausibility..."
    ),
    CALIBRATED(
        cornerNumber = 4,
        stepTitle = "Calibrated",
        instruction = "Room coordinate frame and physical wall models successfully synthesized."
    );

    fun next(): ManualCornerStage {
        return when (this) {
            CORNER_1 -> CORNER_2
            CORNER_2 -> CORNER_3
            CORNER_3 -> CORNER_4
            CORNER_4 -> VALIDATING
            VALIDATING -> CALIBRATED
            CALIBRATED -> CALIBRATED
        }
    }

    fun previous(): ManualCornerStage {
        return when (this) {
            CORNER_1 -> CORNER_1
            CORNER_2 -> CORNER_1
            CORNER_3 -> CORNER_2
            CORNER_4 -> CORNER_3
            VALIDATING -> CORNER_4
            CALIBRATED -> CORNER_4
        }
    }
}
