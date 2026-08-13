package org.asr.studio.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.asr.studio.data.StudioService
import org.asr.studio.protocol.AsrFrame
import org.asr.studio.protocol.AsrVersion
import org.asr.studio.protocol.ConnectionState
import org.asr.studio.protocol.TopicId
import org.asr.studio.currentTimeMillis

class DevicesViewModel(
    private val studioService: StudioService
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = studioService.tcpClient.connectionState

    fun connect(host: String, port: Int) {
        studioService.tcpClient.connect(host, port)
    }

    fun listen(port: Int) {
        studioService.tcpClient.listen(port)
    }

    fun disconnect() {
        studioService.tcpClient.disconnect()
    }

    fun startRuntime() {
        viewModelScope.launch(Dispatchers.IO) {
            println("[DevicesViewModel] Sending 'profile.enable' (full_sensor) command...")
            val json = """{"cmd":"profile.enable","params":{"profile":"full_sensor"},"req_id":${(100..999).random()}}"""
            val payload = json.encodeToByteArray()
            
            val frame = AsrFrame(
                version = AsrVersion.PROTOCOL_VERSION,
                topicId = TopicId.COMMAND.value,
                flags = 0,
                deviceId = 0,
                timestampNs = currentTimeMillis() * 1_000_000,
                seq = 0, 
                payloadLength = payload.size,
                storedCrc = 0,
                computedCrc = 0,
                payload = payload,
                crcValid = true
            )
            studioService.tcpClient.send(frame)
        }
    }
    
    fun stopRuntime() {
        viewModelScope.launch(Dispatchers.IO) {
            println("[DevicesViewModel] stopRuntime() — sending module.stop for all sensor modules...")
            // Send module.stop for each sensor module.
            // We intentionally leave 'transport' and 'diagnostics' running so the
            // TCP connection stays alive and we can send more commands afterwards.
            val sensorModules = listOf("camera", "imu", "gps", "audio", "environment", "battery")
            sensorModules.forEachIndexed { index, moduleId ->
                val reqId = 2000 + index
                val json  = """{"cmd":"module.stop","params":{"id":"$moduleId"},"req_id":$reqId}"""
                val payload = json.encodeToByteArray()
                val frame = AsrFrame(
                    version       = AsrVersion.PROTOCOL_VERSION,
                    topicId       = TopicId.COMMAND.value,
                    flags         = 0,
                    deviceId      = 0,
                    timestampNs   = currentTimeMillis() * 1_000_000,
                    seq           = index,
                    payloadLength = payload.size,
                    storedCrc     = 0,
                    computedCrc   = 0,
                    payload       = payload,
                    crcValid      = true
                )
                studioService.tcpClient.send(frame)
                println("[DevicesViewModel] Sent module.stop for '$moduleId' (req_id=$reqId)")
            }
            println("[DevicesViewModel] stopRuntime() — all module.stop commands sent")
        }
    }
}
