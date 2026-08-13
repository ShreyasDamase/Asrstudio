package org.asr.studio.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuaternionTest {

    @Test
    fun testIdentityQuaternion() {
        val q = Quaternion.IDENTITY
        val euler = q.toEuler()
        assertEquals(0f, euler.pitchDeg, 1e-3f)
        assertEquals(0f, euler.rollDeg, 1e-3f)
        assertEquals(0f, euler.yawDeg, 1e-3f)
    }

    @Test
    fun testCurrentEqualsReferenceGivesZeroRelativeRotation() {
        // Suppose phone is mounted vertically (pitch = 90 deg)
        // rotW=0.7071, rotX=0.7071, rotY=0, rotZ=0
        val qMounted = Quaternion(0.70710678f, 0.70710678f, 0f, 0f).normalized()

        // User taps "Set Current as Zero"
        val qRef = qMounted

        // Relative orientation
        val qRel = qMounted.relativeTo(qRef)
        val eulerRel = qRel.toEuler()

        assertTrue(abs(eulerRel.pitchDeg) < 0.1f, "Pitch relative to zero reference should be ~0°, got ${eulerRel.pitchDeg}")
        assertTrue(abs(eulerRel.rollDeg) < 0.1f, "Roll relative to zero reference should be ~0°, got ${eulerRel.rollDeg}")
        assertTrue(abs(eulerRel.yawDeg) < 0.1f, "Yaw relative to zero reference should be ~0°, got ${eulerRel.yawDeg}")
    }

    @Test
    fun testRelativeMotionAfterZeroing() {
        // Reference pose: Pitch = 90° (0.7071, 0.7071, 0, 0)
        val qRef = Quaternion(0.70710678f, 0.70710678f, 0f, 0f).normalized()

        // Current pose rotated an additional 30° around X axis (Pitch = 120°)
        // 120° / 2 = 60° -> cos(60°) = 0.5, sin(60°) = 0.8660254
        val qCurrent = Quaternion(0.5f, 0.8660254f, 0f, 0f).normalized()

        val qRel = qCurrent.relativeTo(qRef)
        val eulerRel = qRel.toEuler()

        // Expected relative roll around X axis: ~30°
        assertEquals(30f, eulerRel.rollDeg, 0.5f)
    }

    @Test
    fun testQuaternionConjugateAndNorm() {
        val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f)
        assertEquals(1.0f, q.norm(), 1e-4f)

        val qConj = q.conjugate()
        val prod = q * qConj
        assertEquals(1.0f, prod.w, 1e-4f)
        assertEquals(0.0f, prod.x, 1e-4f)
        assertEquals(0.0f, prod.y, 1e-4f)
        assertEquals(0.0f, prod.z, 1e-4f)
    }

    @Test
    fun testRobustnessToNaNAndInf() {
        val qNaN = Quaternion(Float.NaN, 0f, 0f, 0f).normalized()
        assertEquals(Quaternion.IDENTITY, qNaN)

        val qInf = Quaternion(Float.POSITIVE_INFINITY, 0f, 0f, 0f).normalized()
        assertEquals(Quaternion.IDENTITY, qInf)
    }
}
