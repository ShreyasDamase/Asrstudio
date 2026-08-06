package org.asr.studio.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.asr.studio.protocol.AsrFrame
import org.asr.studio.protocol.AsrPacket
import org.asr.studio.protocol.TopicId

data class ImuData(
    val accelX: Float, val accelY: Float, val accelZ: Float,
    val gyroX: Float, val gyroY: Float, val gyroZ: Float,
    val gravityX: Float, val gravityY: Float, val gravityZ: Float,
    val rotW: Float, val rotX: Float, val rotY: Float, val rotZ: Float,
    val timestampNs: Long
)

data class GpsData(
    val lat: Double, val lon: Double, val alt: Float,
    val accuracy: Float, val bearing: Float, val speed: Float,
    val timestampNs: Long
)

data class BatteryData(
    val level: Float,
    val temperature: Float,
    val voltage: Float,
    val isCharging: Boolean,
    val timestampNs: Long
)

class SensorRepository {
    private val _imu = MutableStateFlow<ImuData?>(null)
    val imu: StateFlow<ImuData?> = _imu.asStateFlow()

    private val _gps = MutableStateFlow<GpsData?>(null)
    val gps: StateFlow<GpsData?> = _gps.asStateFlow()

    private val _battery = MutableStateFlow<BatteryData?>(null)
    val battery: StateFlow<BatteryData?> = _battery.asStateFlow()

    fun processPacket(frame: AsrFrame) {
        val topic = TopicId.fromValue(frame.topicId) ?: return
        val payload = frame.payload
        if (payload.isEmpty()) return

        when (topic) {
            TopicId.IMU -> decodeImu(frame)
            TopicId.GPS -> decodeGps(frame)
            TopicId.BATTERY -> decodeBattery(frame)
            else -> { /* Other topics handled elsewhere or ignored */ }
        }
    }

    private fun decodeImu(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 53) return
        // p[0] is payload version (0x01)
        _imu.value = ImuData(
            accelX = readFloatLe(p, 1),
            accelY = readFloatLe(p, 5),
            accelZ = readFloatLe(p, 9),
            gyroX = readFloatLe(p, 13),
            gyroY = readFloatLe(p, 17),
            gyroZ = readFloatLe(p, 21),
            gravityX = readFloatLe(p, 25),
            gravityY = readFloatLe(p, 29),
            gravityZ = readFloatLe(p, 33),
            rotW = readFloatLe(p, 37),
            rotX = readFloatLe(p, 41),
            rotY = readFloatLe(p, 45),
            rotZ = readFloatLe(p, 49),
            timestampNs = frame.timestampNs
        )
    }

    private fun decodeGps(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 33) return
        _gps.value = GpsData(
            lat = readDoubleLe(p, 1),
            lon = readDoubleLe(p, 9),
            alt = readFloatLe(p, 17),
            accuracy = readFloatLe(p, 21),
            bearing = readFloatLe(p, 25),
            speed = readFloatLe(p, 29),
            timestampNs = frame.timestampNs
        )
    }

    private fun decodeBattery(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 17) return
        _battery.value = BatteryData(
            level = readFloatLe(p, 1),
            temperature = readFloatLe(p, 5),
            voltage = readFloatLe(p, 9),
            isCharging = AsrPacket.readIntLe(p, 13) == 1,
            timestampNs = frame.timestampNs
        )
    }

    private fun readFloatLe(bytes: ByteArray, offset: Int): Float {
        val bits = AsrPacket.readIntLe(bytes, offset)
        return Float.fromBits(bits)
    }

    private fun readDoubleLe(bytes: ByteArray, offset: Int): Double {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[offset + i].toLong() and 0xFFL) shl (8 * i))
        }
        return Double.fromBits(value)
    }
}
