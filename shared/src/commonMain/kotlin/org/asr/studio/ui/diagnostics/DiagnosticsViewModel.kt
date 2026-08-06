package org.asr.studio.ui.diagnostics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.asr.studio.data.Diagnostics
import org.asr.studio.data.ImuData
import org.asr.studio.data.StudioService

class DiagnosticsViewModel(
    private val studioService: StudioService
) : ViewModel() {
    val diagnostics: StateFlow<Diagnostics> = studioService.packetRepository.diagnostics
    val imu: StateFlow<ImuData?> = studioService.sensorRepository.imu
}
