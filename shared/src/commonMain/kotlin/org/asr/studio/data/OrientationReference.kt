package org.asr.studio.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.asr.studio.currentTimeMillis
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class OrientationQuaternion(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float,
) {
    val norm: Float
        get() = sqrt(w * w + x * x + y * y + z * z)

    fun normalizedOrNull(tolerance: Float = 0.18f): OrientationQuaternion? {
        if (!w.isFinite() || !x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        val length = norm
        if (!length.isFinite() || length < 0.0001f) return null
        if (abs(length - 1f) > tolerance) return null
        return OrientationQuaternion(w / length, x / length, y / length, z / length)
    }

    fun inverse(): OrientationQuaternion {
        val n2 = w * w + x * x + y * y + z * z
        if (n2 < 0.0000001f) return IdentityQuaternion
        return OrientationQuaternion(w / n2, -x / n2, -y / n2, -z / n2)
    }

    operator fun times(other: OrientationQuaternion): OrientationQuaternion =
        OrientationQuaternion(
            w = w * other.w - x * other.x - y * other.y - z * other.z,
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w,
        )

    fun toEuler(): OrientationEuler {
        val ysqr = y * y
        val t0 = 2.0 * (w * x + y * z)
        val t1 = 1.0 - 2.0 * (x * x + ysqr)
        val roll = atan2(t0, t1)
        val t2 = (2.0 * (w * y - z * x)).coerceIn(-1.0, 1.0)
        val pitch = kotlin.math.asin(t2)
        val t3 = 2.0 * (w * z + x * y)
        val t4 = 1.0 - 2.0 * (ysqr + z * z)
        val yaw = atan2(t3, t4)
        return OrientationEuler(
            pitchDeg = (pitch * 180.0 / PI).toFloat(),
            rollDeg = (roll * 180.0 / PI).toFloat(),
            yawDeg = normalizeSignedDegrees((yaw * 180.0 / PI).toFloat()),
        )
    }

    fun approximatelyEquals(other: OrientationQuaternion, epsilon: Float = 0.001f): Boolean =
        abs(w - other.w) <= epsilon &&
            abs(x - other.x) <= epsilon &&
            abs(y - other.y) <= epsilon &&
            abs(z - other.z) <= epsilon
}

data class OrientationEuler(
    val pitchDeg: Float,
    val rollDeg: Float,
    val yawDeg: Float,
)

data class OrientationReference(
    val quaternion: OrientationQuaternion,
    val savedAtMs: Long? = null,
)

enum class OrientationReferenceStatus {
    NotSet,
    Session,
    Saved,
}

data class OrientationReferenceState(
    val status: OrientationReferenceStatus = OrientationReferenceStatus.NotSet,
    val reference: OrientationReference? = null,
    val message: String? = null,
) {
    val isActive: Boolean
        get() = reference != null
}

data class CalibratedOrientation(
    val rawQuaternion: OrientationQuaternion,
    val relativeQuaternion: OrientationQuaternion,
    val rawEuler: OrientationEuler,
    val relativeEuler: OrientationEuler,
    val referenceStatus: OrientationReferenceStatus,
)

interface OrientationReferenceStore {
    fun load(): OrientationReference?
    fun save(reference: OrientationReference)
    fun clear()
}

class InMemoryOrientationReferenceStore : OrientationReferenceStore {
    private var reference: OrientationReference? = null

    override fun load(): OrientationReference? = reference

    override fun save(reference: OrientationReference) {
        this.reference = reference
    }

    override fun clear() {
        reference = null
    }
}

class OrientationReferenceRepository(
    private val store: OrientationReferenceStore = InMemoryOrientationReferenceStore(),
) {
    private val _state = MutableStateFlow(OrientationReferenceState())
    val state: StateFlow<OrientationReferenceState> = _state.asStateFlow()

    fun setCurrentAsZero(imu: ImuData?): Boolean {
        val quaternion = imu.toOrientationQuaternionOrNull()
        if (quaternion == null) {
            _state.value = _state.value.copy(message = "Cannot set reference: latest quaternion is invalid.")
            return false
        }
        _state.value = OrientationReferenceState(
            status = OrientationReferenceStatus.Session,
            reference = OrientationReference(quaternion),
            message = "Session reference set from current orientation.",
        )
        return true
    }

    fun resetReference() {
        _state.value = OrientationReferenceState(message = "Reference reset. Raw sensor orientation is shown.")
    }

    fun saveReference(): Boolean {
        val active = _state.value.reference ?: return false.also {
            _state.value = _state.value.copy(message = "No active reference to save.")
        }
        val saved = active.copy(savedAtMs = currentTimeMillis())
        store.save(saved)
        _state.value = OrientationReferenceState(
            status = OrientationReferenceStatus.Saved,
            reference = saved,
            message = "Reference saved as mount calibration.",
        )
        return true
    }

    fun loadReference(): Boolean {
        val saved = store.load()
        if (saved == null) {
            _state.value = _state.value.copy(message = "No saved mount calibration found.")
            return false
        }
        val quaternion = saved.quaternion.normalizedOrNull()
        if (quaternion == null) {
            _state.value = _state.value.copy(message = "Saved mount calibration is invalid and was not loaded.")
            return false
        }
        _state.value = OrientationReferenceState(
            status = OrientationReferenceStatus.Saved,
            reference = saved.copy(quaternion = quaternion),
            message = "Saved mount calibration loaded.",
        )
        return true
    }

    fun clearSavedReference() {
        store.clear()
        if (_state.value.status == OrientationReferenceStatus.Saved) {
            resetReference()
        } else {
            _state.value = _state.value.copy(message = "Saved mount calibration cleared.")
        }
    }

    fun calibratedOrientation(imu: ImuData?): CalibratedOrientation? {
        val current = imu.toOrientationQuaternionOrNull() ?: return null
        val reference = _state.value.reference?.quaternion?.normalizedOrNull()
        val relative = if (reference == null) current else (reference.inverse() * current).normalizedOrNull(tolerance = 1f) ?: return null
        return CalibratedOrientation(
            rawQuaternion = current,
            relativeQuaternion = relative,
            rawEuler = current.toEuler(),
            relativeEuler = relative.toEuler(),
            referenceStatus = _state.value.status,
        )
    }
}

fun ImuData?.toOrientationQuaternionOrNull(): OrientationQuaternion? {
    if (this == null) return null
    return OrientationQuaternion(rotW, rotX, rotY, rotZ).normalizedOrNull()
}

fun relativeOrientation(reference: OrientationQuaternion, current: OrientationQuaternion): OrientationQuaternion? {
    val normalizedReference = reference.normalizedOrNull() ?: return null
    val normalizedCurrent = current.normalizedOrNull() ?: return null
    return (normalizedReference.inverse() * normalizedCurrent).normalizedOrNull(tolerance = 1f)
}

fun quaternionFromAxisAngle(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): OrientationQuaternion {
    val axisLength = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
    if (axisLength < 0.0001f) return IdentityQuaternion
    val halfRadians = degrees * PI.toFloat() / 360f
    val s = kotlin.math.sin(halfRadians)
    return OrientationQuaternion(
        w = kotlin.math.cos(halfRadians),
        x = axisX / axisLength * s,
        y = axisY / axisLength * s,
        z = axisZ / axisLength * s,
    )
}

private fun normalizeSignedDegrees(degrees: Float): Float {
    var value = degrees
    while (value > 180f) value -= 360f
    while (value <= -180f) value += 360f
    return value
}

val IdentityQuaternion = OrientationQuaternion(1f, 0f, 0f, 0f)
