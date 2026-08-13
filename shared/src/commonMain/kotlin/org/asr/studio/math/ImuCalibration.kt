package org.asr.studio.math

import org.asr.studio.data.ImuData

enum class ReferenceState {
    NOT_SET,
    SESSION_SET,
    SAVED
}

data class OrientationReference(
    val state: ReferenceState = ReferenceState.NOT_SET,
    val referenceQuaternion: Quaternion = Quaternion.IDENTITY,
    val timestampMs: Long = 0L
)

data class CalibratedImuData(
    val raw: ImuData,
    val rawQuaternion: Quaternion,
    val relativeQuaternion: Quaternion,
    val rawEuler: EulerAngles,
    val relativeEuler: EulerAngles,
    val isCalibrated: Boolean
)
