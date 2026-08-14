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
    val magX: Float = 0f, val magY: Float = 0f, val magZ: Float = 0f,
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

data class LightSensorData(
    val lux: Float,
    val timestampNs: Long
)

data class FlashlightState(
    val isOn: Boolean,
    val brightness: Float,
    val timestampNs: Long
)

data class MicData(
    val rmsLevel: Float,
    val peakLevel: Float,
    val noiseFloor: Float,
    val voiceActive: Boolean,
    val sampleRateHz: Int,
    val timestampNs: Long
)
class SensorRepository {
    private val _imu = MutableStateFlow<ImuData?>(null)
    val imu: StateFlow<ImuData?> = _imu.asStateFlow()

    private val _lightSensor = MutableStateFlow<LightSensorData?>(null)
    val lightSensor: StateFlow<LightSensorData?> = _lightSensor.asStateFlow()

    private val _flashlight = MutableStateFlow<FlashlightState?>(null)
    val flashlight: StateFlow<FlashlightState?> = _flashlight.asStateFlow()

    private val _mic = MutableStateFlow<MicData?>(null)
    val mic: StateFlow<MicData?> = _mic.asStateFlow()

    private val _orientationReference = MutableStateFlow(org.asr.studio.math.OrientationReference())
    val orientationReference: StateFlow<org.asr.studio.math.OrientationReference> = _orientationReference.asStateFlow()

    private val _calibratedImu = MutableStateFlow<org.asr.studio.math.CalibratedImuData?>(null)
    val calibratedImu: StateFlow<org.asr.studio.math.CalibratedImuData?> = _calibratedImu.asStateFlow()

    private val _gps = MutableStateFlow<GpsData?>(null)
    val gps: StateFlow<GpsData?> = _gps.asStateFlow()

    private val _battery = MutableStateFlow<BatteryData?>(null)
    val battery: StateFlow<BatteryData?> = _battery.asStateFlow()

    // Persistent hardware module power states (survives screen navigation)
    private val _modulePowerStates = MutableStateFlow<Map<String, Boolean>>(
        mapOf(
            "camera" to true,
            "imu" to true,
            "gps" to true,
            "audio" to true,
            "speaker" to true,
            "environment" to true
        )
    )
    val modulePowerStates: StateFlow<Map<String, Boolean>> = _modulePowerStates.asStateFlow()

    fun setModulePower(moduleId: String, active: Boolean) {
        _modulePowerStates.value = _modulePowerStates.value.toMutableMap().apply {
            put(moduleId, active)
        }
        if (moduleId == "audio" && !active) {
            _mic.value = null
        }
    }

    fun clearMic() {
        _mic.value = null
    }

    fun setAllModulesPower(active: Boolean) {
        _modulePowerStates.value = mapOf(
            "camera" to active,
            "imu" to active,
            "gps" to active,
            "audio" to active,
            "speaker" to active,
            "environment" to active
        )
        if (!active) {
            _mic.value = null
        }
    }

    fun setZero() {
        val currentImu = _imu.value ?: return
        val qCurr = org.asr.studio.math.Quaternion(
            w = currentImu.rotW,
            x = currentImu.rotX,
            y = currentImu.rotY,
            z = currentImu.rotZ
        ).normalized()

        if (!qCurr.isFinite()) return

        _orientationReference.value = org.asr.studio.math.OrientationReference(
            state = org.asr.studio.math.ReferenceState.SESSION_SET,
            referenceQuaternion = qCurr,
            timestampMs = org.asr.studio.currentTimeMillis()
        )
        recomputeCalibratedImu(currentImu)
    }

    fun resetReference() {
        _orientationReference.value = org.asr.studio.math.OrientationReference(
            state = org.asr.studio.math.ReferenceState.NOT_SET,
            referenceQuaternion = org.asr.studio.math.Quaternion.IDENTITY,
            timestampMs = org.asr.studio.currentTimeMillis()
        )
        _imu.value?.let { recomputeCalibratedImu(it) }
    }

    fun saveMountCalibration() {
        val currentRef = _orientationReference.value
        if (currentRef.state != org.asr.studio.math.ReferenceState.NOT_SET) {
            _orientationReference.value = currentRef.copy(
                state = org.asr.studio.math.ReferenceState.SAVED,
                timestampMs = org.asr.studio.currentTimeMillis()
            )
        }
    }

    fun processPacket(frame: AsrFrame) {
        val topic = TopicId.fromValue(frame.topicId) ?: return
        val payload = frame.payload
        if (payload.isEmpty()) return

        when (topic) {
            TopicId.IMU -> decodeImu(frame)
            TopicId.GPS -> decodeGps(frame)
            TopicId.BATTERY -> decodeBattery(frame)
            TopicId.AUDIO -> decodeMic(frame)
            TopicId.AUDIO_RAW -> decodeRawAudio(frame)
            TopicId.FLASHLIGHT -> decodeFlashlight(frame)
            TopicId.LIGHT_SENSOR -> decodeLightSensor(frame)
            else -> { /* Other topics handled elsewhere or ignored */ }
        }
    }

    var isLiveSpeakerOutputEnabled: Boolean = false

    private fun readShortLe(b: ByteArray, offset: Int): Short {
        return ((b[offset].toInt() and 0xFF) or ((b[offset + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    private fun decodeRawAudio(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 3) return
        if (isLiveSpeakerOutputEnabled) {
            val sampleCount = readShortLe(p, 1).toInt()
            val byteCount = sampleCount * 2
            if (p.size >= 3 + byteCount) {
                org.asr.studio.playAudioPcm(p, 3, byteCount)
            }
        }
    }

    private fun decodeImu(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 53) return
        val rawData = ImuData(
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
            magX = if (p.size >= 65) readFloatLe(p, 53) else 0f,
            magY = if (p.size >= 65) readFloatLe(p, 57) else 0f,
            magZ = if (p.size >= 65) readFloatLe(p, 61) else 0f,
            timestampNs = frame.timestampNs
        )
        _imu.value = rawData
        recomputeCalibratedImu(rawData)
    }

    private fun decodeMic(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 14) return
        _mic.value = MicData(
            rmsLevel = readFloatLe(p, 1).coerceIn(0f, 1f),
            peakLevel = readFloatLe(p, 5).coerceIn(0f, 1f),
            noiseFloor = readFloatLe(p, 9),
            voiceActive = p[13].toInt() != 0,
            sampleRateHz = if (p.size >= 18) AsrPacket.readIntLe(p, 14) else 16000,
            timestampNs = frame.timestampNs
        )
    }

    private fun decodeFlashlight(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 6) return
        _flashlight.value = FlashlightState(
            isOn = p[1].toInt() != 0,
            brightness = readFloatLe(p, 2).coerceIn(0f, 1f),
            timestampNs = frame.timestampNs
        )
    }

    private fun decodeLightSensor(frame: AsrFrame) {
        val p = frame.payload
        if (p.size < 5) return
        _lightSensor.value = LightSensorData(
            lux = readFloatLe(p, 1).coerceAtLeast(0f),
            timestampNs = frame.timestampNs
        )
    }

    private fun recomputeCalibratedImu(rawData: ImuData) {
        val qRaw = org.asr.studio.math.Quaternion(
            w = rawData.rotW,
            x = rawData.rotX,
            y = rawData.rotY,
            z = rawData.rotZ
        ).normalized()

        val ref = _orientationReference.value
        val isCalibrated = ref.state != org.asr.studio.math.ReferenceState.NOT_SET
        val qRel = if (isCalibrated) qRaw.relativeTo(ref.referenceQuaternion) else qRaw

        _calibratedImu.value = org.asr.studio.math.CalibratedImuData(
            raw = rawData,
            rawQuaternion = qRaw,
            relativeQuaternion = qRel,
            rawEuler = qRaw.toEuler(),
            relativeEuler = qRel.toEuler(),
            isCalibrated = isCalibrated
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
