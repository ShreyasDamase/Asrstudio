package org.asr.studio.math

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt

data class EulerAngles(
    val pitchDeg: Float,
    val rollDeg: Float,
    val yawDeg: Float,
    val pitchRad: Float,
    val rollRad: Float,
    val yawRad: Float,
)

/**
 * Immutable strongly-typed Quaternion representation [w, x, y, z].
 */
data class Quaternion(
    val w: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    fun conjugate(): Quaternion = Quaternion(w, -x, -y, -z)

    fun norm(): Float = sqrt(w * w + x * x + y * y + z * z)

    fun isFinite(): Boolean = !w.isNaN() && !x.isNaN() && !y.isNaN() && !z.isNaN() &&
            !w.isInfinite() && !x.isInfinite() && !y.isInfinite() && !z.isInfinite()

    fun normalized(): Quaternion {
        val n = norm()
        return if (isFinite() && n > 1e-6f) {
            Quaternion(w / n, x / n, y / n, z / n)
        } else {
            IDENTITY
        }
    }

    /**
     * Quaternion multiplication: q_this * q_other
     * Representing composition of rotations.
     */
    operator fun times(q: Quaternion): Quaternion = Quaternion(
        w = w * q.w - x * q.x - y * q.y - z * q.z,
        x = w * q.x + x * q.w + y * q.z - z * q.y,
        y = w * q.y - x * q.z + y * q.w + z * q.x,
        z = w * q.z + x * q.y - y * q.x + z * q.w
    ).normalized()

    /**
     * Calculates the relative orientation quaternion from reference to current.
     * q_relative = q_reference.conjugate() * q_current
     */
    fun relativeTo(reference: Quaternion): Quaternion {
        val refNorm = reference.normalized()
        val currNorm = this.normalized()
        return (refNorm.conjugate() * currNorm).normalized()
    }

    /**
     * Converts quaternion to Euler Angles (Pitch, Roll, Yaw) in degrees and radians.
     */
    fun toEuler(): EulerAngles {
        val q = this.normalized()
        val ysqr = q.y * q.y
        val t0 = 2.0 * (q.w * q.x + q.y * q.z)
        val t1 = 1.0 - 2.0 * (q.x * q.x + ysqr)
        val roll = atan2(t0, t1)

        val t2 = (2.0 * (q.w * q.y - q.z * q.x)).coerceIn(-1.0, 1.0)
        val pitch = asin(t2)

        val t3 = 2.0 * (q.w * q.z + q.x * q.y)
        val t4 = 1.0 - 2.0 * (ysqr + q.z * q.z)
        val yaw = atan2(t3, t4)

        return EulerAngles(
            pitchDeg = (pitch * 180.0 / PI).toFloat(),
            rollDeg = (roll * 180.0 / PI).toFloat(),
            yawDeg = ((yaw * 180.0 / PI + 360.0) % 360.0).toFloat(),
            pitchRad = pitch.toFloat(),
            rollRad = roll.toFloat(),
            yawRad = yaw.toFloat()
        )
    }

    companion object {
        val IDENTITY = Quaternion(1f, 0f, 0f, 0f)
    }
}
