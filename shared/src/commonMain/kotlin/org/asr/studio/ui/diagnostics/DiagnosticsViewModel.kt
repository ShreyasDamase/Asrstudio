package org.asr.studio.ui.diagnostics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.asr.studio.data.Diagnostics
import org.asr.studio.data.GpsData
import org.asr.studio.data.ImuData
import org.asr.studio.data.StudioService

class DiagnosticsViewModel(
    private val studioService: StudioService
) : ViewModel() {
    val diagnostics: StateFlow<Diagnostics> = studioService.packetRepository.diagnostics
    val imu: StateFlow<ImuData?> = studioService.sensorRepository.imu
    val gps: StateFlow<GpsData?> = studioService.sensorRepository.gps
    val calibratedImu: StateFlow<org.asr.studio.math.CalibratedImuData?> = studioService.sensorRepository.calibratedImu
    val orientationReference: StateFlow<org.asr.studio.math.OrientationReference> = studioService.sensorRepository.orientationReference

    fun setZero() {
        studioService.sensorRepository.setZero()
    }

    fun resetReference() {
        studioService.sensorRepository.resetReference()
    }

    fun saveMountCalibration() {
        studioService.sensorRepository.saveMountCalibration()
    }
}
