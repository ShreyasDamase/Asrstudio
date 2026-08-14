package org.asr.studio.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.asr.studio.currentTimeMillis
import org.asr.studio.data.CameraFrameData
import org.asr.studio.data.CameraRepository
import org.asr.studio.data.FlashlightState
import org.asr.studio.data.LightSensorData
import org.asr.studio.data.StudioService
import org.asr.studio.protocol.AsrFrame
import org.asr.studio.protocol.AsrVersion
import org.asr.studio.protocol.ConnectionState
import org.asr.studio.protocol.TopicId

class CameraViewModel(
    private val studioService: StudioService = StudioService.instance,
    private val cameraRepository: CameraRepository = studioService.cameraRepository
) : ViewModel() {

    val uiState: StateFlow<CameraUiState> = cameraRepository.latestFrame.map { frame ->
        if (frame == null) {
            val connectionState = studioService.tcpClient.connectionState.value
            if (connectionState == ConnectionState.Connected) {
                CameraUiState.WaitingForPackets
            } else {
                CameraUiState.WaitingForRuntime
            }
        } else {
            CameraUiState.Streaming(frame)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CameraUiState.WaitingForRuntime
    )

    val lightSensor: StateFlow<LightSensorData?> = studioService.sensorRepository.lightSensor
    val flashlight: StateFlow<FlashlightState?> = studioService.sensorRepository.flashlight

    private var flashlightOn = false
    private var flashlightBrightness = 1.0f

    fun toggleFlashlight() {
        flashlightOn = !flashlightOn
        sendFlashlightCommand(flashlightOn, flashlightBrightness)
    }

    fun setFlashlightBrightness(level: Float) {
        flashlightBrightness = level.coerceIn(0f, 1f)
        if (flashlightOn) sendFlashlightCommand(true, flashlightBrightness)
    }

    private fun sendFlashlightCommand(isOn: Boolean, brightness: Float) {
        val now = currentTimeMillis()
        val json = """{"cmd":"flashlight.set","params":{"on":$isOn,"brightness":${"%.2f".format(brightness)}},"req_id":${now % 10000}}"""
        val payload = json.encodeToByteArray()
        val frame = AsrFrame(
            version = AsrVersion.PROTOCOL_VERSION,
            topicId = TopicId.COMMAND.value,
            flags = 0,
            deviceId = 0,
            timestampNs = now * 1_000_000,
            seq = (now % Int.MAX_VALUE).toInt(),
            payloadLength = payload.size,
            storedCrc = 0,
            computedCrc = 0,
            payload = payload,
            crcValid = true
        )
        viewModelScope.launch(Dispatchers.IO) {
            studioService.tcpClient.send(frame)
        }
    }
}

sealed class CameraUiState {
    data object WaitingForRuntime : CameraUiState()
    data object WaitingForPackets : CameraUiState()
    data class Streaming(val frame: CameraFrameData) : CameraUiState()
}
