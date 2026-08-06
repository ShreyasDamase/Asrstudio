package org.asr.studio.ui.devices

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.asr.studio.data.StudioService
import org.asr.studio.protocol.ConnectionState

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
}
