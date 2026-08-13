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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.asr.studio.protocol.ConnectMode

class DevicesViewModel(
    private val studioService: StudioService
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = studioService.tcpClient.connectionState
    val activeMode: StateFlow<ConnectMode?> = studioService.tcpClient.activeMode

    var mode by mutableStateOf(ConnectMode.WIFI)
    var host by mutableStateOf("")
    var port by mutableStateOf("4242")
    var adbStatusMessage by mutableStateOf<String?>(null)

    fun autoSetupAdb(portVal: Int = 4242) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = org.asr.studio.runAdbReverseAutoSetup(portVal)
            adbStatusMessage = result
            if (result.startsWith("✅")) {
                listen("127.0.0.1", portVal)
            }
        }
    }

    fun connect(host: String, port: Int) {
        studioService.tcpClient.connect(host, port)
    }

    fun listen(host: String = "0.0.0.0", port: Int = 4242) {
        studioService.tcpClient.listen(host, port)
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
            println("[DevicesViewModel] Sending 'runtime.stop' command...")
            val reqId = 9999
            val json  = """{"cmd":"runtime.stop","params":{},"req_id":$reqId}"""
            val payload = json.encodeToByteArray()
            val frame = AsrFrame(
                version       = AsrVersion.PROTOCOL_VERSION,
                topicId       = TopicId.COMMAND.value,
                flags         = 0,
                deviceId      = 0,
                timestampNs   = currentTimeMillis() * 1_000_000,
                seq           = 0,
                payloadLength = payload.size,
                storedCrc     = 0,
                computedCrc   = 0,
                payload       = payload,
                crcValid      = true
            )
            studioService.tcpClient.send(frame)
            println("[DevicesViewModel] Sent 'runtime.stop' command (req_id=$reqId)")
        }
    }
}
