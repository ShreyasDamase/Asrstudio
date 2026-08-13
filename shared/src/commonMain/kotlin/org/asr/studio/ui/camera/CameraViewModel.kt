package org.asr.studio.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.asr.studio.data.CameraFrameData
import org.asr.studio.data.CameraRepository
import org.asr.studio.data.StudioService
import org.asr.studio.protocol.ConnectionState

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
}

sealed class CameraUiState {
    data object WaitingForRuntime : CameraUiState()
    data object WaitingForPackets : CameraUiState()
    data class Streaming(val frame: CameraFrameData) : CameraUiState()
}
