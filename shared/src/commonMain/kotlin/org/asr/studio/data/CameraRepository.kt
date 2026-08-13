package org.asr.studio.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.asr.studio.currentTimeMillis
import org.asr.studio.decodeJpeg
import org.asr.studio.protocol.AsrFrame

data class CameraFrameData(
    val frameId: Long,
    val width: Int,
    val height: Int,
    val encoding: Byte,
    val quality: Byte,
    val exposureNs: Long,
    val iso: Int,
    val sensorTimestampNs: Long,
    val image: ImageBitmap?,
    val fps: Float,
    val latencyMs: Long
)

class CameraRepository {
    private val _latestFrame = MutableStateFlow<CameraFrameData?>(null)
    val latestFrame: StateFlow<CameraFrameData?> = _latestFrame.asStateFlow()

    private var frameCount = 0
    private var lastFpsCalculationTime = 0L
    private var currentFps = 0f

    fun processPacket(frame: AsrFrame) {
        if (frame.topicId != 0x01.toByte()) return // TopicId.CAMERA_FRAME

        val payload = frame.payload
        if (payload.size < 31) return
        val version = payload[0]
        if (version != 0x01.toByte()) return

        val width = readUInt16Le(payload, 1)
        val height = readUInt16Le(payload, 3)
        val encoding = payload[5]
        val quality = payload[6]
        val frameId = readUInt32Le(payload, 7)
        val exposureNs = readLongLe(payload, 11)
        val iso = readUInt16Le(payload, 19)
        val sensorTimestampNs = readLongLe(payload, 23)
        val jpegData = payload.copyOfRange(31, payload.size)

        val now = currentTimeMillis()
        val latencyMs = now - (sensorTimestampNs / 1_000_000)

        // FPS calculation
        frameCount++
        if (lastFpsCalculationTime == 0L) {
            lastFpsCalculationTime = now
        } else if (now - lastFpsCalculationTime >= 1000) {
            currentFps = frameCount * 1000f / (now - lastFpsCalculationTime)
            frameCount = 0
            lastFpsCalculationTime = now
        }

        val bitmap = decodeJpeg(jpegData)

        _latestFrame.value = CameraFrameData(
            frameId = frameId,
            width = width,
            height = height,
            encoding = encoding,
            quality = quality,
            exposureNs = exposureNs,
            iso = iso,
            sensorTimestampNs = sensorTimestampNs,
            image = bitmap,
            fps = currentFps,
            latencyMs = latencyMs
        )
    }

    private fun readUInt16Le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readUInt32Le(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 0xFFL) or
         ((bytes[offset + 1].toLong() and 0xFFL) shl 8) or
         ((bytes[offset + 2].toLong() and 0xFFL) shl 16) or
         ((bytes[offset + 3].toLong() and 0xFFL) shl 24))

    private fun readLongLe(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[offset + i].toLong() and 0xFFL) shl (8 * i))
        }
        return value
    }
}
